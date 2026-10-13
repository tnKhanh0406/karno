package com.karnoorder.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoorder.component.InventoryReservationEventFactory;
import com.karnoorder.constants.CheckoutEventConstants;
import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.dto.response.SagaStartResult;
import com.karnoorder.entity.CheckoutSaga;
import com.karnoorder.enums.CheckoutSagaStatus;
import com.karnoorder.exception.CheckoutException;
import com.karnoorder.messaging.payload.InventoryReservationRequestedPayload;
import com.karnoorder.repository.CheckoutSagaRepo;
import com.karnoorder.service.CheckoutSagaStartTxService;
import com.karnoorder.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CheckoutSagaStartTxServiceImpl implements CheckoutSagaStartTxService {
    private final CheckoutSagaRepo checkoutSagaRepo;
    private final InventoryReservationEventFactory eventFactory;
    private final OutboxEventService outboxService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SagaStartResult persistNewOrGetExisting(CheckoutContext context, String idempotencyKey, String requestHash) {
        String contextJson = serializeContext(context);

        CheckoutSaga saga = CheckoutSaga.builder()
                .id(context.checkoutId())
                .customerId(context.customerId())
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(CheckoutSagaStatus.PROCESSING)
                .contextPayload(contextJson)
                .version(0L)
                .build();

        int inserted = checkoutSagaRepo.insertIfAbsent(saga);

        if (inserted == 0) {
            CheckoutSaga existing =
                    checkoutSagaRepo.findByCustomerAndKey(
                            context.customerId(),
                            idempotencyKey
                    );

            if (existing == null) {
                throw new IllegalStateException("Saga conflict detected but existing row was not found");
            }

            validateSameRequest(existing, requestHash);
            return new SagaStartResult(existing, false);
        }

        if (inserted != 1) {
            throw new IllegalStateException("Unexpected checkout saga insert count: " + inserted);
        }

        InventoryReservationRequestedPayload payload = eventFactory.build(context);

        outboxService.enqueueEvent(
                CheckoutEventConstants.AGGREGATE_TYPE,
                context.checkoutId(),
                CheckoutEventConstants.INVENTORY_RESERVATION_REQUESTED,
                CheckoutEventConstants.INVENTORY_RESERVATION_REQUESTED_TOPIC,
                payload
        );

        return new SagaStartResult(saga, true);
    }


    private String serializeContext(CheckoutContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize CheckoutContext", ex);
        }
    }


    private void validateSameRequest(CheckoutSaga existing, String requestHash) {
        if (!Objects.equals(existing.getRequestHash(), requestHash)) {
            throw new CheckoutException(
                    HttpStatus.CONFLICT,
                    "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key đã được sử dụng cho một request khác"
            );
        }
    }
}
