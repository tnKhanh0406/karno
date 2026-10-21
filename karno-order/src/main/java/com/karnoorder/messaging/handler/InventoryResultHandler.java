package com.karnoorder.messaging.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.entity.CheckoutSaga;
import com.karnoorder.entity.Order;
import com.karnoorder.entity.OrderItem;
import com.karnoorder.enums.CheckoutSagaStatus;
import com.karnoorder.enums.OrderStatus;
import com.karnoorder.enums.PaymentStatus;
import com.karnoorder.messaging.payload.InventoryConfirmationRequestedPayload;
import com.karnoorder.messaging.payload.InventoryReleasedPayload;
import com.karnoorder.messaging.payload.InventoryReservationFailedPayload;
import com.karnoorder.messaging.payload.InventoryReservationExpiredPayload;
import com.karnoorder.messaging.payload.InventoryReservedPayload;
import com.karnoorder.exception.UnrecoverableOrderCreationException;
import com.karnoorder.repository.*;
import com.karnoorder.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryResultHandler {

    private static final String CONSUMER_NAME = "order-inventory-result-handler";
    private static final String CONFIRM_EVENT_TYPE = "INVENTORY_CONFIRMATION_REQUESTED";
    private static final String CONFIRM_TOPIC = "inventory.confirmation.requested";

    private final ProcessedEventRepo processedEventRepo;
    private final CheckoutSagaRepo checkoutSagaRepo;
    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;
    private final OrderStatusHistoryRepo historyRepo;
    private final CartItemRepo cartItemRepo;
    private final OutboxEventService outboxService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void onReserved(UUID eventId, InventoryReservedPayload payload) {
        int inserted =
                processedEventRepo.insertIfAbsent(
                        eventId,
                        CONSUMER_NAME,
                        "INVENTORY_RESERVED"
                );

        if (inserted == 0) {
            log.info("Duplicate INVENTORY_RESERVED, eventId={}", eventId);
            return;
        }

        CheckoutSaga saga = checkoutSagaRepo.findByIdForUpdate(payload.checkoutId());

        if (saga == null) {
            throw new IllegalStateException("Checkout Saga not found: " + payload.checkoutId());
        }
        if (saga.getStatus() == CheckoutSagaStatus.ORDER_COMMITTED
                || saga.getStatus() == CheckoutSagaStatus.COMPLETED) {
            log.info(
                    "Checkout already committed, checkoutId={}",
                    payload.checkoutId()
            );
            return;
        }
        if (saga.getStatus() != CheckoutSagaStatus.PROCESSING) {
            throw new IllegalStateException("Unexpected Saga status for INVENTORY_RESERVED: " + saga.getStatus());
        }

        CheckoutContext context = deserializeContext(saga.getContextPayload());

        if (!Objects.equals(context.checkoutId(), payload.checkoutId())) {
            throw new UnrecoverableOrderCreationException("Checkout ID mismatch");
        }
        if (!Objects.equals(context.customerId(), saga.getCustomerId())) {
            throw new UnrecoverableOrderCreationException("Customer ID mismatch");
        }

        createOrders(context);
        removeCheckoutCartItems(context);

        int updated = checkoutSagaRepo.markOrderCommitted(context.checkoutId());

        if (updated != 1) {
            throw new IllegalStateException("Cannot mark Saga ORDER_COMMITTED");
        }

        InventoryConfirmationRequestedPayload confirmPayload = new InventoryConfirmationRequestedPayload(context.checkoutId());

        outboxService.enqueueEvent(
                "CHECKOUT",
                context.checkoutId(),
                CONFIRM_EVENT_TYPE,
                CONFIRM_TOPIC,
                confirmPayload
        );

        log.info(
                "Stage 8 success: orders created, checkoutId={}",
                context.checkoutId()
        );
    }

    @Transactional
    public void onReservationFailed(UUID eventId, InventoryReservationFailedPayload payload) {
        int inserted =
                processedEventRepo.insertIfAbsent(
                        eventId,
                        CONSUMER_NAME,
                        "INVENTORY_RESERVATION_FAILED"
                );

        if (inserted == 0) {
            log.info("Duplicate reservation failure, eventId={}", eventId);
            return;
        }

        CheckoutSaga saga = checkoutSagaRepo.findByIdForUpdate(payload.checkoutId());

        if (saga == null) {
            throw new IllegalStateException("Checkout Saga not found: " + payload.checkoutId());
        }
        if (saga.getStatus() == CheckoutSagaStatus.FAILED) {
            return;
        }
        if (saga.getStatus() != CheckoutSagaStatus.PROCESSING) {
            throw new IllegalStateException("Unexpected Saga status for reservation failure: " + saga.getStatus());
        }

        String failureCode = "INVENTORY_RESERVATION_FAILED";
        String failureMessage = "Không thể giữ hàng";

        int updated =
                checkoutSagaRepo.markFailed(
                        payload.checkoutId(),
                        failureCode,
                        failureMessage
                );

        if (updated != 1) {
            throw new IllegalStateException("Cannot mark Saga FAILED");
        }

        log.info(
                "Stage 8 reservation failed, checkoutId={}, code={}",
                payload.checkoutId(),
                failureCode
        );
    }

    @Transactional
    public void onReservationExpired(UUID eventId, InventoryReservationExpiredPayload payload) {
        if (processedEventRepo.insertIfAbsent(
                eventId, CONSUMER_NAME, "INVENTORY_RESERVATION_EXPIRED") == 0) {
            return;
        }

        CheckoutSaga saga = checkoutSagaRepo.findByIdForUpdate(payload.checkoutId());
        if (saga == null) {
            throw new IllegalStateException("Checkout Saga not found: " + payload.checkoutId());
        }
        if (saga.getStatus() == CheckoutSagaStatus.FAILED) {
            return;
        }
        if (saga.getStatus() == CheckoutSagaStatus.COMPENSATING) {
            if (checkoutSagaRepo.markFailedAfterRelease(payload.checkoutId()) != 1) {
                throw new IllegalStateException("Cannot complete expired compensation");
            }
            return;
        }
        if (saga.getStatus() != CheckoutSagaStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Reservation expired after checkout commit: " + saga.getStatus());
        }
        if (checkoutSagaRepo.markFailed(
                payload.checkoutId(),
                "INVENTORY_RESERVATION_EXPIRED",
                "Thời gian giữ hàng đã hết") != 1) {
            throw new IllegalStateException("Cannot mark expired checkout FAILED");
        }
    }

    @Transactional
    public void onReleased(UUID eventId, InventoryReleasedPayload payload) {
        int inserted =
                processedEventRepo.insertIfAbsent(
                        eventId,
                        CONSUMER_NAME,
                        "INVENTORY_RELEASED"
                );

        if (inserted == 0) {
            log.info("Skip duplicated INVENTORY_RELEASED eventId={}", eventId);
            return;
        }

        CheckoutSaga saga = checkoutSagaRepo.findByIdForUpdate(payload.checkoutId());

        if (saga == null) {
            throw new IllegalStateException("Checkout Saga not found: " + payload.checkoutId());
        }
        if (saga.getStatus() == CheckoutSagaStatus.FAILED) {
            return;
        }
        if (saga.getStatus() != CheckoutSagaStatus.COMPENSATING) {
            throw new IllegalStateException("Unexpected Saga status for INVENTORY_RELEASED: " + saga.getStatus());
        }

        int updated = checkoutSagaRepo.markFailedAfterRelease(payload.checkoutId());

        if (updated != 1) {
            throw new IllegalStateException("Cannot complete Inventory compensation");
        }

        log.info(
                "Inventory compensation completed checkoutId={}",
                payload.checkoutId()
        );
    }

    private CheckoutContext deserializeContext(String json) {
        try {
            return objectMapper.readValue(json, CheckoutContext.class);
        } catch (JsonProcessingException ex) {
            throw new UnrecoverableOrderCreationException("Cannot deserialize CheckoutContext", ex);
        }
    }

    private void createOrders(CheckoutContext context) {
        if (context.orders() == null || context.orders().isEmpty()) {
            throw new UnrecoverableOrderCreationException("CheckoutContext has no OrderDraft");
        }

        for (CheckoutContext.OrderDraft draft : context.orders()) {
            if (draft.items() == null || draft.items().isEmpty() || draft.shipping() == null) {
                throw new UnrecoverableOrderCreationException("Invalid OrderDraft: " + draft.orderId());
            }

            Order order = toOrder(context, draft);
            int orderInserted = orderRepo.insert(order);
            if (orderInserted != 1) {
                throw new UnrecoverableOrderCreationException("Cannot insert Order: " + draft.orderId());
            }

            for (CheckoutContext.OrderItemDraft item : draft.items()) {

                OrderItem itemRow = toOrderItem(draft.orderId(), item);
                int itemInserted = orderItemRepo.insert(itemRow);
                if (itemInserted != 1) {
                    throw new UnrecoverableOrderCreationException("Cannot insert OrderItem: " + item.orderItemId());
                }
            }

            int historyInserted =
                    historyRepo.insertInitial(
                            UUID.randomUUID(),
                            draft.orderId(),
                            "PENDING"
                    );

            if (historyInserted != 1) {
                throw new UnrecoverableOrderCreationException("Cannot insert Order history: " + draft.orderId());
            }
        }
    }

    private void removeCheckoutCartItems(CheckoutContext context) {

        for (CheckoutContext.OrderDraft order : context.orders()) {
            for (CheckoutContext.OrderItemDraft item : order.items()) {
                int deleted = cartItemRepo.deleteIfUnchanged(
                        context.customerId(),
                        item.cartItemId(),
                        item.variantId(),
                        item.quantity()
                );

                if (deleted == 0) {
                    log.info(
                            "Cart item changed or removed, cartItemId={}",
                            item.cartItemId()
                    );
                }
            }
        }
    }

    private Order toOrder(CheckoutContext context, CheckoutContext.OrderDraft draft) {
        Order order = new Order();
        order.setId(draft.orderId());
        order.setCustomerId(context.customerId());
        order.setCheckoutId(context.checkoutId());
        order.setReceiverName(context.receiver().name());
        order.setReceiverAddress(context.receiver().address());
        order.setReceiverPhone(context.receiver().phone());
        order.setShopId(draft.shopId());
        order.setShopName(draft.shopName());
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(draft.subtotal());
        order.setShippingFee(draft.shipping().shippingFee());
        order.setTotalAmount(draft.totalAmount());
        order.setShippingMethod(draft.shipping().shippingMethod());
        order.setPaymentMethod(context.paymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDeliverySlotStart(context.deliverySlotStart() != null ? context.deliverySlotStart().atOffset(java.time.ZoneOffset.UTC) : null);
        order.setDeliverySlotEnd(context.deliverySlotEnd() != null ? context.deliverySlotEnd().atOffset(java.time.ZoneOffset.UTC) : null);
        order.setNote(context.note());
        return order;
    }

    private OrderItem toOrderItem(UUID orderId, CheckoutContext.OrderItemDraft itemDraft) {
        OrderItem item = new OrderItem();
        item.setId(itemDraft.orderItemId());
        item.setOrderId(orderId);
        item.setProductId(itemDraft.productId());
        item.setVariantId(itemDraft.variantId());
        item.setProductName(itemDraft.productName());
        item.setVariantName(itemDraft.variantName());
        item.setSku(itemDraft.sku());
        item.setProductImageUrl(itemDraft.productImageUrl());
        item.setUnitPrice(itemDraft.unitPrice());
        item.setQuantity(itemDraft.quantity());
        item.setSubtotal(itemDraft.subtotal());
        item.setWeightGram(itemDraft.weightGram());
        item.setLengthCm(itemDraft.lengthCm());
        item.setWidthCm(itemDraft.widthCm());
        item.setHeightCm(itemDraft.heightCm());
        return item;
    }
}
