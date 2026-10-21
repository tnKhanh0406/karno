package com.karnoinventory.messaging.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoinventory.constant.EventTopics;
import com.karnoinventory.constant.EventTypes;
import com.karnoinventory.entity.Inventory;
import com.karnoinventory.entity.InventoryMovement;
import com.karnoinventory.entity.InventoryReservation;
import com.karnoinventory.enums.InventoryActorType;
import com.karnoinventory.enums.InventoryMovementType;
import com.karnoinventory.enums.InventoryReferenceType;
import com.karnoinventory.enums.ReservationStatus;
import com.karnoinventory.messaging.model.EventEnvelope;
import com.karnoinventory.messaging.payload.InventoryReleaseRequestedPayload;
import com.karnoinventory.messaging.payload.InventoryReleasedPayload;
import com.karnoinventory.repository.InventoryMovementRepo;
import com.karnoinventory.repository.InventoryRepo;
import com.karnoinventory.repository.InventoryReservationRepo;
import com.karnoinventory.repository.ProcessedEventRepo;
import com.karnoinventory.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryReleaseRequestedHandler {
    private static final String CONSUMER_NAME = "inventory-release-requested-handler";
    private static final String REQUEST_TYPE = "INVENTORY_RELEASE_REQUESTED";

    private final ProcessedEventRepo processedEventRepo;
    private final InventoryReservationRepo reservationRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryMovementRepo movementRepo;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(EventEnvelope event) {
        if (event == null || event.eventId() == null || event.data() == null
                || !REQUEST_TYPE.equals(event.eventType())) {
            throw new IllegalArgumentException("Invalid inventory release event");
        }

        InventoryReleaseRequestedPayload payload = objectMapper.convertValue(
                event.data(), InventoryReleaseRequestedPayload.class);
        if (payload == null || payload.checkoutId() == null
                || !payload.checkoutId().equals(event.aggregateId())) {
            throw new IllegalArgumentException("Invalid inventory release checkoutId");
        }

        if (processedEventRepo.insertIfAbsent(event.eventId(), CONSUMER_NAME, REQUEST_TYPE) == 0) {
            return;
        }

        List<InventoryReservation> reservations =
                reservationRepo.findByCheckoutIdForUpdate(payload.checkoutId());
        if (reservations == null || reservations.isEmpty()) {
            throw new IllegalStateException("No reservations for checkoutId=" + payload.checkoutId());
        }

        boolean allReleased = reservations.stream()
                .allMatch(r -> r.getStatus() == ReservationStatus.RELEASED
                        || r.getStatus() == ReservationStatus.EXPIRED);
        if (!allReleased) {
            boolean allActive = reservations.stream()
                    .allMatch(r -> r.getStatus() == ReservationStatus.ACTIVE);
            if (!allActive) {
                throw new IllegalStateException(
                        "Cannot release reservations in mixed or committed states, checkoutId="
                                + payload.checkoutId());
            }
            releaseActiveReservations(reservations, payload.reason());
        }

        outboxService.enqueueEvent(
                "CHECKOUT",
                payload.checkoutId(),
                EventTypes.INVENTORY_RELEASED,
                EventTopics.INVENTORY_RELEASED,
                new InventoryReleasedPayload(payload.checkoutId())
        );
    }

    private void releaseActiveReservations(List<InventoryReservation> reservations, String reason) {
        for (InventoryReservation reservation : reservations) {
            Inventory inventory = inventoryRepo.findByIdForUpdate(reservation.getInventoryId());
            if (inventory == null) {
                throw new IllegalStateException("Inventory not found: " + reservation.getInventoryId());
            }

            int before = inventory.getReservedQuantity();
            if (inventoryRepo.decreaseReserved(inventory.getId(), reservation.getQuantity()) != 1) {
                throw new IllegalStateException("Cannot release inventory: " + inventory.getId());
            }
            if (reservationRepo.markReleased(reservation.getId()) != 1) {
                throw new IllegalStateException("Cannot release reservation: " + reservation.getId());
            }

            movementRepo.insert(InventoryMovement.builder()
                    .id(UUID.randomUUID())
                    .inventoryId(inventory.getId())
                    .type(InventoryMovementType.RELEASE_RESERVATION)
                    .quantity(reservation.getQuantity())
                    .onHandBefore(inventory.getOnHandQuantity())
                    .onHandAfter(inventory.getOnHandQuantity())
                    .reservedBefore(before)
                    .reservedAfter(before - reservation.getQuantity())
                    .referenceType(InventoryReferenceType.ORDER)
                    .referenceId(reservation.getOrderItemId())
                    .reason(reason == null ? "Checkout compensation" : reason)
                    .actorType(InventoryActorType.SYSTEM)
                    .build());
        }
    }
}
