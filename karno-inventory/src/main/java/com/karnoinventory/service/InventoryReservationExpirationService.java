package com.karnoinventory.service;

import com.karnoinventory.constant.EventTopics;
import com.karnoinventory.constant.EventTypes;
import com.karnoinventory.entity.Inventory;
import com.karnoinventory.entity.InventoryMovement;
import com.karnoinventory.entity.InventoryReservation;
import com.karnoinventory.enums.InventoryActorType;
import com.karnoinventory.enums.InventoryMovementType;
import com.karnoinventory.enums.InventoryReferenceType;
import com.karnoinventory.enums.ReservationStatus;
import com.karnoinventory.messaging.payload.InventoryReservationExpiredPayload;
import com.karnoinventory.repository.InventoryMovementRepo;
import com.karnoinventory.repository.InventoryRepo;
import com.karnoinventory.repository.InventoryReservationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryReservationExpirationService {
    private final InventoryReservationRepo reservationRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryMovementRepo movementRepo;
    private final OutboxService outboxService;

    @Transactional
    public void expireCheckout(UUID checkoutId, Instant now) {
        List<InventoryReservation> reservations = reservationRepo.findByCheckoutIdForUpdate(checkoutId);
        if (reservations == null || reservations.isEmpty()) {
            return;
        }

        List<InventoryReservation> active = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.ACTIVE)
                .toList();
        if (active.isEmpty() || active.stream().anyMatch(r -> r.getExpiresAt().isAfter(now))) {
            return;
        }

        for (InventoryReservation reservation : active) {
            Inventory inventory = inventoryRepo.findByIdForUpdate(reservation.getInventoryId());
            if (inventory == null) {
                throw new IllegalStateException("Inventory not found: " + reservation.getInventoryId());
            }
            int before = inventory.getReservedQuantity();
            if (inventoryRepo.decreaseReserved(inventory.getId(), reservation.getQuantity()) != 1
                    || reservationRepo.markExpired(reservation.getId()) != 1) {
                throw new IllegalStateException("Cannot expire reservation: " + reservation.getId());
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
                    .reason("Inventory reservation expired")
                    .actorType(InventoryActorType.SYSTEM)
                    .build());
        }

        outboxService.enqueueEvent(
                "CHECKOUT", checkoutId,
                EventTypes.INVENTORY_RESERVATION_EXPIRED,
                EventTopics.INVENTORY_RESERVATION_EXPIRED,
                new InventoryReservationExpiredPayload(checkoutId)
        );
    }
}
