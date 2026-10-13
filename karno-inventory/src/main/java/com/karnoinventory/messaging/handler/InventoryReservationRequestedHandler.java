package com.karnoinventory.messaging.handler;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.karnoinventory.messaging.model.ReservationValidationFailure;
import com.karnoinventory.messaging.payload.InventoryReservationFailedPayload;
import com.karnoinventory.messaging.payload.InventoryReservationRequestedPayload;
import com.karnoinventory.messaging.payload.InventoryReservedPayload;
import com.karnoinventory.repository.InventoryMovementRepo;
import com.karnoinventory.repository.InventoryRepo;
import com.karnoinventory.repository.InventoryReservationRepo;
import com.karnoinventory.repository.ProcessedEventRepo;
import com.karnoinventory.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationRequestedHandler {

    private static final String CONSUMER_NAME = "inventory-reservation-requested-handler";
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(15);

    private final ProcessedEventRepo processedEventRepo;
    private final InventoryRepo inventoryRepo;
    private final InventoryReservationRepo reservationRepo;
    private final InventoryMovementRepo movementRepo;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(EventEnvelope event) {
        int inserted = processedEventRepo.insertIfAbsent(
                            event.eventId(),
                            CONSUMER_NAME,
                            event.eventType());

        if (inserted == 0) {
            log.info(
                    "Duplicate event skipped. eventId={}, consumer={}",
                    event.eventId(),
                    CONSUMER_NAME
            );
            return;
        }

        InventoryReservationRequestedPayload payload = objectMapper.convertValue(event.data(), InventoryReservationRequestedPayload.class);
        validatePayload(payload);

        Map<UUID, Integer> quantityByVariant = buildQuantityVariantMap(payload);

        List<Inventory> inventories = inventoryRepo.findByVariantIdsForUpdate(quantityByVariant.keySet());
        Map<UUID, Inventory> inventoryByVariant = inventories.stream()
                .collect(Collectors.toMap(Inventory::getVariantId, i -> i));

        ReservationValidationFailure failure = validateStock(quantityByVariant, inventoryByVariant);
        if (failure != null) {
            publishReservationFailed(payload.checkoutId(), failure);
            return;
        }

        Instant expiresAt = Instant.now().plus(RESERVATION_TTL);
        List<InventoryReservedPayload.Item> reservedItems = new ArrayList<>();

        for (InventoryReservationRequestedPayload.Item item : payload.items()) {
            Inventory inventory = inventoryByVariant.get(item.variantId());

            int beforeReserved = inventory.getReservedQuantity();
            int afterReserved = beforeReserved + item.quantity();

            int updated = inventoryRepo.increaseReserved(inventory.getId(), item.quantity());
            if (updated != 1) {
                throw new IllegalStateException("Failed to reserve inventory " + inventory.getId());
            }

            UUID reservationId = UUID.randomUUID();
            InventoryReservation reservation = InventoryReservation.builder()
                            .id(reservationId)
                            .inventoryId(inventory.getId())
                            .checkoutId(payload.checkoutId())
                            .orderId(item.orderId())
                            .orderItemId(item.orderItemId())
                            .quantity(item.quantity())
                            .status(ReservationStatus.ACTIVE)
                            .expiresAt(expiresAt)
                            .build();

            reservationRepo.insert(reservation);

            InventoryMovement movement = InventoryMovement.builder()
                            .id(UUID.randomUUID())
                            .inventoryId(inventory.getId())
                            .type(InventoryMovementType.RESERVE)
                            .quantity(item.quantity())
                            .onHandBefore(inventory.getOnHandQuantity())
                            .onHandAfter(inventory.getOnHandQuantity())
                            .reservedBefore(beforeReserved)
                            .reservedAfter(afterReserved)
                            .referenceType(InventoryReferenceType.ORDER)
                            .referenceId(item.orderItemId())
                            .reason("Checkout inventory reservation")
                            .actorType(InventoryActorType.SYSTEM)
                            .build();

            movementRepo.insert(movement);

            inventory.setReservedQuantity(afterReserved);


            reservedItems.add(
                    new InventoryReservedPayload.Item(
                            reservationId,
                            item.orderId(),
                            item.orderItemId(),
                            item.variantId(),
                            item.quantity()
                    )
            );
        }

        InventoryReservedPayload reservedPayload =
                new InventoryReservedPayload(
                        payload.checkoutId(),
                        reservedItems
                );

        outboxService.enqueueEvent(
                "CHECKOUT",
                payload.checkoutId(),
                EventTypes.INVENTORY_RESERVED,
                EventTopics.INVENTORY_RESERVED,
                reservedPayload
        );

        log.info(
                "Processing new InventoryReservationRequested event. eventId={}, checkoutId={}",
                event.eventId(),
                event.aggregateId()
        );
    }

    private static Map<UUID, Integer> buildQuantityVariantMap(InventoryReservationRequestedPayload payload) {
        Map<UUID, Integer> quantityByVariant = new HashMap<>();
        for (InventoryReservationRequestedPayload.Item item : payload.items()) {
            if (quantityByVariant.containsKey(item.variantId())) {
                quantityByVariant.put(
                        item.variantId(),
                        quantityByVariant.get(item.variantId()) + item.quantity()
                );
            } else {
                quantityByVariant.put(item.variantId(), item.quantity());
            }
        }
        return quantityByVariant;
    }

    private InventoryReservationRequestedPayload parsePayload(JsonNode data) {
        return objectMapper.convertValue(
                data,
                InventoryReservationRequestedPayload.class
        );
    }

    private void validatePayload(InventoryReservationRequestedPayload payload) {
        if (payload == null
                || payload.checkoutId() == null
                || payload.items() == null
                || payload.items().isEmpty()) {
            throw new IllegalArgumentException("Invalid inventory reservation payload");
        }

        for (InventoryReservationRequestedPayload.Item item : payload.items()) {

            if (item.orderId() == null
                    || item.orderItemId() == null
                    || item.variantId() == null
                    || item.quantity() == null
                    || item.quantity() <= 0) {

                throw new IllegalArgumentException("Invalid inventory reservation item");
            }
        }
    }

    private ReservationValidationFailure validateStock(
            Map<UUID, Integer> quantityByVariant,
            Map<UUID, Inventory> inventoryByVariant) {

        for (Map.Entry<UUID, Integer> entry : quantityByVariant.entrySet()) {
            UUID variantId = entry.getKey();
            int requested = entry.getValue();

            Inventory inventory = inventoryByVariant.get(variantId);

            if (inventory == null) {
                return new ReservationValidationFailure(
                        "INVENTORY_NOT_FOUND",
                        variantId,
                        requested,
                        0,
                        "Inventory not found for variant " + variantId
                );
            }

            int available = inventory.getOnHandQuantity() - inventory.getReservedQuantity();

            if (available < requested) {
                return new ReservationValidationFailure(
                        "INSUFFICIENT_STOCK",
                        variantId,
                        requested,
                        available,
                        "Insufficient stock for variant " + variantId
                );
            }
        }
        return null;
    }

    private void publishReservationFailed(
            UUID checkoutId,
            ReservationValidationFailure failure) {

        InventoryReservationFailedPayload payload =
                new InventoryReservationFailedPayload(
                        checkoutId,
                        failure.code(),
                        failure.message(),
                        failure.variantId(),
                        failure.requestedQuantity(),
                        failure.availableQuantity()
                );

        outboxService.enqueueEvent(
                "CHECKOUT",
                checkoutId,
                EventTypes.INVENTORY_RESERVATION_FAILED,
                EventTopics.INVENTORY_RESERVATION_FAILED,
                payload
        );
    }
}
