package com.karnoorder.service.impl;

import com.karnoorder.component.CheckoutRequestFingerprint;
import com.karnoorder.dto.request.CreateOrderRequest;
import com.karnoorder.dto.request.PackOrderRequest;
import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.dto.response.CheckoutStartResponse;
import com.karnoorder.dto.response.CheckoutStatusResponse;
import com.karnoorder.dto.response.SagaStartResult;
import com.karnoorder.dto.response.PackOrderResponse;
import com.karnoorder.entity.Order;
import com.karnoorder.enums.OrderStatus;
import com.karnoorder.messaging.payload.OrderPackedPayload;
import com.karnoorder.repository.OrderRepo;
import com.karnoorder.repository.OrderStatusHistoryRepo;
import com.karnoorder.service.OutboxEventService;
import com.karnoorder.constants.EventTopics;
import com.karnoorder.constants.EventTypes;
import com.karnoorder.entity.CheckoutSaga;
import com.karnoorder.exception.CheckoutException;
import com.karnoorder.repository.CheckoutSagaRepo;
import com.karnoorder.service.CheckoutPreparationService;
import com.karnoorder.service.CheckoutSagaStartTxService;
import com.karnoorder.service.OrderApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderApplicationServiceImpl implements OrderApplicationService {
    private final CheckoutSagaRepo checkoutSagaRepo;
    private final CheckoutPreparationService checkoutPreparationService;
    private final CheckoutRequestFingerprint requestFingerprint;
    private final CheckoutSagaStartTxService checkoutSagaTxService;
    private final OrderRepo orderRepo;
    private final OrderStatusHistoryRepo orderStatusHistoryRepo;
    private final OutboxEventService outboxEventService;

    @Override
    public CheckoutStartResponse createOrder(
            UUID customerId,
            String idempotencyKey,
            CreateOrderRequest request
    ) {
        String key = normalizeIdempotencyKey(idempotencyKey);
        String requestHash = requestFingerprint.hash(request);

        CheckoutSaga existing =
                checkoutSagaRepo.findByCustomerAndKey(
                        customerId,
                        key
                );

        if (existing != null) {
            validateSameRequest(existing, requestHash);
            return toResponse(existing, true);
        }

        CheckoutContext context =
                checkoutPreparationService.prepare(
                        customerId,
                        request
                );

        SagaStartResult result =
                checkoutSagaTxService.persistNewOrGetExisting(
                        context,
                        key,
                        requestHash
                );

        return toResponse(result.saga(), !result.created());
    }

    @Override
    public CheckoutStatusResponse getCheckoutStatus(UUID customerId, UUID checkoutId) {
        CheckoutSaga saga =
                checkoutSagaRepo.findByIdAndCustomer(
                        checkoutId,
                        customerId
                );

        if (saga == null) {
            throw new CheckoutException(
                    HttpStatus.NOT_FOUND,
                    "CHECKOUT_NOT_FOUND",
                    "Không tìm thấy yêu cầu đặt hàng"
            );
        }

        return new CheckoutStatusResponse(
                saga.getId(),
                saga.getStatus(),
                saga.getFailureCode(),
                saga.getCompletedAt()
        );
    }

    @Override
    @Transactional
    public PackOrderResponse packOrder(UUID orderId, PackOrderRequest request) {
        Order order = orderRepo.findById(orderId);
        if (order == null || !request.shopId().equals(order.getShopId())) {
            throw new CheckoutException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                    "Không tìm thấy order thuộc shop này");
        }

        if (order.getStatus() == OrderStatus.PACKED) {
            return new PackOrderResponse(orderId, OrderStatus.PACKED, true);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new CheckoutException(HttpStatus.CONFLICT, "ORDER_CANNOT_BE_PACKED",
                    "Chỉ order PENDING mới có thể chuyển sang PACKED");
        }

        OrderStatus previousStatus = order.getStatus();
        if (orderRepo.markPacked(orderId, request.shopId()) != 1) {
            Order current = orderRepo.findById(orderId);
            if (current != null && current.getStatus() == OrderStatus.PACKED) {
                return new PackOrderResponse(orderId, OrderStatus.PACKED, true);
            }
            throw new CheckoutException(HttpStatus.CONFLICT, "ORDER_STATUS_CHANGED",
                    "Trạng thái order đã thay đổi, vui lòng tải lại dữ liệu");
        }

        Instant packedAt = Instant.now();
        if (orderStatusHistoryRepo.insertTransition(
                UUID.randomUUID(), orderId, previousStatus.name(), OrderStatus.PACKED.name(),
                request.sellerId(), "Seller confirmed that the order was packed") != 1) {
            throw new IllegalStateException("Cannot insert PACKED order history");
        }
        outboxEventService.enqueueEvent(
                "ORDER", orderId, EventTypes.ORDER_PACKED, EventTopics.ORDER_PACKED,
                new OrderPackedPayload(orderId, order.getShopId(), request.sellerId(), packedAt));

        return new PackOrderResponse(orderId, OrderStatus.PACKED, false);
    }


    private String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw new CheckoutException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key phải có từ 1 đến 100 ký tự"
            );
        }

        String key = value.trim();

        if (key.isEmpty()) {
            throw new CheckoutException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key không hợp lệ"
            );
        }

        return key;
    }


    private void validateSameRequest(CheckoutSaga existing, String requestHash) {
        if (!Objects.equals(existing.getRequestHash(), requestHash)) {
            throw new CheckoutException(
                    HttpStatus.CONFLICT,
                    "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key đã được sử dụng cho nội dung khác"
            );
        }
    }

    private CheckoutStartResponse toResponse(CheckoutSaga saga, boolean reused) {
        return new CheckoutStartResponse(
                saga.getId(),
                saga.getStatus(),
                reused,
                "Trạng thái yêu cầu đặt hàng: " + saga.getStatus().name()
        );
    }
}
