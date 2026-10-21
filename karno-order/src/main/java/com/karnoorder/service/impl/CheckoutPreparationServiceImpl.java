package com.karnoorder.service.impl;

import com.karnoorder.component.CheckoutContextFactory;
import com.karnoorder.component.CheckoutDataLoader;
import com.karnoorder.component.ShippingRateService;
import com.karnoorder.constants.AggregateTypes;
import com.karnoorder.constants.EventTopics;
import com.karnoorder.constants.EventTypes;
import com.karnoorder.dto.request.CreateOrderRequest;
import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.entity.CheckoutSaga;
import com.karnoorder.enums.CheckoutSagaStatus;
import com.karnoorder.messaging.payload.InventoryReservationRequestedPayload;
import com.karnoorder.repository.CheckoutSagaRepo;
import com.karnoorder.service.CheckoutPreparationService;
import com.karnoorder.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutPreparationServiceImpl implements CheckoutPreparationService {
    private final CheckoutSagaRepo checkoutSagaRepo;
    private final OutboxEventService outboxService;
    private final CheckoutDataLoader dataLoader;
    private final ShippingRateService shippingRateService;
    private final CheckoutContextFactory contextFactory;

    @Override
    @Transactional
    public UUID startTestSaga(
            UUID customerId,
            UUID variantId,
            Integer quantity,
            String idempotencyKey
    ) {
        UUID checkoutId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();

        CheckoutSaga checkoutSaga = CheckoutSaga.builder()
                .id(checkoutId)
                .customerId(customerId)
                .idempotencyKey(idempotencyKey)
                .status(CheckoutSagaStatus.PROCESSING)
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
                AggregateTypes.CHECKOUT,
                checkoutId,
                EventTypes.INVENTORY_RESERVATION_REQUESTED,
                EventTopics.INVENTORY_RESERVATION_REQUESTED,
                payload
        );

        return checkoutId;
    }

    @Override
    public CheckoutContext prepare(UUID customerId, CreateOrderRequest request) {
        CheckoutDataLoader.CheckoutData data =
                dataLoader.load(customerId, request);
        ShippingRateService.ShippingResult shipping =
                shippingRateService.calculate(data);

        return contextFactory.create(customerId, request, data, shipping);
    }
}
