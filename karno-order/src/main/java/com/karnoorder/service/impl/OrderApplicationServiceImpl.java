package com.karnoorder.service.impl;

import com.karnoorder.component.CheckoutRequestFingerprint;
import com.karnoorder.dto.request.CreateOrderRequest;
import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.dto.response.CheckoutStartResponse;
import com.karnoorder.dto.response.CheckoutStatusResponse;
import com.karnoorder.dto.response.SagaStartResult;
import com.karnoorder.entity.CheckoutSaga;
import com.karnoorder.exception.CheckoutException;
import com.karnoorder.repository.CheckoutSagaRepo;
import com.karnoorder.service.CheckoutPreparationService;
import com.karnoorder.service.CheckoutSagaStartTxService;
import com.karnoorder.service.OrderApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderApplicationServiceImpl implements OrderApplicationService {
    private final CheckoutSagaRepo checkoutSagaRepo;
    private final CheckoutPreparationService checkoutPreparationService;
    private final CheckoutRequestFingerprint requestFingerprint;
    private final CheckoutSagaStartTxService checkoutSagaTxService;

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
