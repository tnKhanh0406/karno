package com.karnoorder.messaging.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoorder.component.ShipmentCreationEventFactory;
import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.entity.CheckoutSaga;
import com.karnoorder.enums.CheckoutSagaStatus;
import com.karnoorder.messaging.payload.InventoryConfirmedPayload;
import com.karnoorder.messaging.payload.ShipmentCreationRequestedPayload;
import com.karnoorder.repository.CheckoutSagaRepo;
import com.karnoorder.repository.OrderRepo;
import com.karnoorder.repository.ProcessedEventRepo;
import com.karnoorder.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryConfirmedHandler {
    private static final String CONSUMER_NAME = "order-inventory-confirmed-handler";
    private static final String EVENT_TYPE = "INVENTORY_CONFIRMED";
    private static final String SHIPMENT_EVENT_TYPE = "SHIPMENT_CREATION_REQUESTED";
    private static final String SHIPMENT_TOPIC = "shipping.shipment.creation.requested";

    private final ProcessedEventRepo processedEventRepo;
    private final CheckoutSagaRepo checkoutSagaRepo;
    private final OrderRepo orderRepo;
    private final OutboxEventService outboxService;
    private final ShipmentCreationEventFactory shipmentFactory;
    private final ObjectMapper objectMapper;


    @Transactional
    public void handle(UUID eventId, InventoryConfirmedPayload payload) {
        if (eventId == null
                || payload == null
                || payload.checkoutId() == null) {

            throw new IllegalArgumentException("Invalid INVENTORY_CONFIRMED event");
        }

        UUID checkoutId = payload.checkoutId();
        log.info("[STAGE 9] Start InventoryConfirmed checkoutId={}", checkoutId);

        int inserted = processedEventRepo.insertIfAbsent(
                eventId,
                CONSUMER_NAME,
                EVENT_TYPE
        );

        if (inserted == 0) {
            log.info("[STAGE 9] Duplicate eventId={}", eventId);
            return;
        }

        CheckoutSaga saga = checkoutSagaRepo.findByIdForUpdate(checkoutId);

        if (saga == null) {
            throw new IllegalStateException("Checkout Saga not found: " + checkoutId);
        }

        if (saga.getStatus() == CheckoutSagaStatus.COMPLETED) {
            log.info("[STAGE 9] Saga already COMPLETED checkoutId={}", checkoutId);
            return;
        }

        if (saga.getStatus() != CheckoutSagaStatus.ORDER_COMMITTED) {
            throw new IllegalStateException("Unexpected Saga status for INVENTORY_CONFIRMED: " + saga.getStatus());
        }

        CheckoutContext context = deserializeContext(saga.getContextPayload());

        if (!checkoutId.equals(context.checkoutId())
                || !saga.getCustomerId().equals(context.customerId())) {
            throw new IllegalStateException("CheckoutContext does not match Saga");
        }

        if (context.orders() == null
                || context.orders().isEmpty()) {
            throw new IllegalStateException("CheckoutContext contains no orders");
        }

        int existingOrderCount = orderRepo.countByCheckoutId(checkoutId);

        if (existingOrderCount != context.orders().size()) {
            throw new IllegalStateException(
                    "Orders count mismatch, expected="
                            + context.orders().size()
                            + ", actual="
                            + existingOrderCount
            );
        }

        // 8. Saga -> COMPLETED
        int updated = checkoutSagaRepo.markCompleted(checkoutId);
        if (updated != 1) {
            throw new IllegalStateException("Cannot mark Saga COMPLETED");
        }

        for (CheckoutContext.OrderDraft order : context.orders()) {
            ShipmentCreationRequestedPayload shipmentPayload = shipmentFactory.build(context, order);
            outboxService.enqueueEvent(
                    "ORDER",
                    order.orderId(),
                    SHIPMENT_EVENT_TYPE,
                    SHIPMENT_TOPIC,
                    shipmentPayload
            );

            log.info(
                    "[STAGE 9] Shipment creation enqueued orderId={}",
                    order.orderId()
            );
        }

        log.info(
                "[STAGE 9] Checkout Saga COMPLETED checkoutId={}, shipmentCount={}",
                checkoutId,
                context.orders().size()
        );
    }

    private CheckoutContext deserializeContext(String json) {
        try {
            return objectMapper.readValue(
                    json,
                    CheckoutContext.class
            );

        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Cannot deserialize CheckoutContext",
                    ex
            );
        }
    }
}
