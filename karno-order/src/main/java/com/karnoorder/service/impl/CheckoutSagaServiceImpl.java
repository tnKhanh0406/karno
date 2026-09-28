package com.karnoorder.service.impl;

import com.karnoorder.constants.EventTopics;
import com.karnoorder.constants.EventTypes;
import com.karnoorder.entity.CheckoutSaga;
import com.karnoorder.messaging.payload.InventoryReservationRequestedPayload;
import com.karnoorder.repository.CheckoutSagaRepo;
import com.karnoorder.service.CheckoutSagaService;
import com.karnoorder.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutSagaServiceImpl implements CheckoutSagaService {
    private final CheckoutSagaRepo checkoutSagaRepo;
    private final OutboxEventService outboxService;

    @Override
    @Transactional
    public UUID startTestSaga(UUID customerId, UUID variantId, Integer quantity, String idempotencyKey) {
        UUID checkoutId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();

        CheckoutSaga checkoutSaga = CheckoutSaga.builder()
                .id(checkoutId)
                .customerId(customerId)
                .idempotencyKey(idempotencyKey)
                .status("PROCESSING")
                .contextPayload("{}")
                .version(0L)
                .build();

        checkoutSagaRepo.insert(checkoutSaga);

        InventoryReservationRequestedPayload payload =
                new InventoryReservationRequestedPayload(
                        checkoutId,
                        List.of(
                                new InventoryReservationRequestedPayload.Item(
                                        orderId,
                                        orderItemId,
                                        variantId,
                                        quantity
                                )
                        )
                );

        outboxService.enqueueEvent(
                "CHECKOUT",
                checkoutId,
                EventTypes.INVENTORY_RESERVATION_REQUESTED,
                EventTopics.INVENTORY_RESERVATION_REQUESTED,
                payload
        );

        return checkoutId;
    }
}
