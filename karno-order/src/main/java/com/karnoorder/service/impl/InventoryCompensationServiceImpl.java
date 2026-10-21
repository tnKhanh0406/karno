package com.karnoorder.service.impl;

import com.karnoorder.entity.CheckoutSaga;
import com.karnoorder.enums.CheckoutSagaStatus;
import com.karnoorder.messaging.payload.InventoryReleaseRequestedPayload;
import com.karnoorder.repository.CheckoutSagaRepo;
import com.karnoorder.service.InventoryCompensationService;
import com.karnoorder.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryCompensationServiceImpl implements InventoryCompensationService {

    private final CheckoutSagaRepo checkoutSagaRepo;
    private final OutboxEventService outboxService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startCompensation(UUID checkoutId, String failureCode, String failureMessage) {
        CheckoutSaga saga = checkoutSagaRepo.findByIdForUpdate(checkoutId);

        if (saga == null) {
            throw new IllegalStateException("Checkout Saga not found: " + checkoutId);
        }

        if (saga.getStatus() == CheckoutSagaStatus.COMPENSATING) {
            log.info("Compensation already started checkoutId={}", checkoutId);
            return;
        }

        if (saga.getStatus() == CheckoutSagaStatus.FAILED) {
            return;
        }

        if (saga.getStatus() == CheckoutSagaStatus.ORDER_COMMITTED
                || saga.getStatus() == CheckoutSagaStatus.COMPLETED) {
            log.warn(
                    "Cannot compensate committed checkoutId={}, status={}",
                    checkoutId,
                    saga.getStatus()
            );
            return;
        }

        if (saga.getStatus() != CheckoutSagaStatus.PROCESSING) {
            throw new IllegalStateException("Unexpected saga status: " + saga.getStatus());
        }

        int updated = checkoutSagaRepo.markCompensating(
                checkoutId,
                failureCode,
                failureMessage
        );

        if (updated != 1) {
            throw new IllegalStateException("Cannot mark Saga COMPENSATING");
        }

        InventoryReleaseRequestedPayload payload = new InventoryReleaseRequestedPayload(checkoutId, "ORDER_CREATION_FAILED");

        outboxService.enqueueEvent(
                "CHECKOUT",
                checkoutId,
                "INVENTORY_RELEASE_REQUESTED",
                "inventory.release.requested",
                payload
        );

        log.info("Inventory compensation started checkoutId={}", checkoutId);
    }
}
