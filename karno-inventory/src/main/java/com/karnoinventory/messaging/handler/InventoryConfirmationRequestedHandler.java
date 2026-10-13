package com.karnoinventory.messaging.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoinventory.entity.Inventory;
import com.karnoinventory.entity.InventoryReservation;
import com.karnoinventory.enums.ReservationStatus;
import com.karnoinventory.messaging.model.EventEnvelope;
import com.karnoinventory.messaging.payload.InventoryConfirmationRequestedPayload;
import com.karnoinventory.messaging.payload.InventoryConfirmedPayload;
import com.karnoinventory.repository.InventoryRepo;
import com.karnoinventory.repository.InventoryReservationRepo;
import com.karnoinventory.repository.ProcessedEventRepo;
import com.karnoinventory.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryConfirmationRequestedHandler {

    private static final String CONSUMER_NAME = "inventory-confirmation-handler";
    private static final String REQUEST_EVENT_TYPE = "INVENTORY_CONFIRMATION_REQUESTED";
    private static final String RESPONSE_EVENT_TYPE = "INVENTORY_CONFIRMED";
    private static final String RESPONSE_TOPIC = "inventory.confirmed";

    private final ProcessedEventRepo processedEventRepo;
    private final InventoryReservationRepo reservationRepo;
    private final InventoryRepo inventoryRepo;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(EventEnvelope event) {
        if (event == null
                || event.eventId() == null
                || event.data() == null
                || !REQUEST_EVENT_TYPE.equals(event.eventType())) {

            throw new IllegalArgumentException("Invalid inventory confirmation event");
        }

        InventoryConfirmationRequestedPayload payload = readPayload(event.data());

        UUID checkoutId = payload.checkoutId();

        if (checkoutId == null || !checkoutId.equals(event.aggregateId())) {
            throw new IllegalArgumentException("Invalid confirmation checkoutId");
        }

        int inserted = processedEventRepo.insertIfAbsent(
                event.eventId(),
                CONSUMER_NAME,
                REQUEST_EVENT_TYPE
        );

        if (inserted == 0) {
            log.info("Duplicate confirmation eventId={}", event.eventId());
            return;
        }

        List<InventoryReservation> reservations = reservationRepo.findByCheckoutIdForUpdate(checkoutId);

        if (reservations == null || reservations.isEmpty()) {
            throw new IllegalStateException("No inventory reservations for checkoutId=" + checkoutId);
        }

        boolean allConfirmed = reservations.stream()
                .allMatch(r -> r.getStatus() == ReservationStatus.CONFIRMED);

        if (allConfirmed) {
            log.info("Checkout inventory already confirmed: {}", checkoutId);
            return;
        }

        boolean allActive = reservations.stream()
                .allMatch(r -> r.getStatus() == ReservationStatus.ACTIVE);

        if (!allActive) {
            throw new IllegalStateException(
                    "Cannot confirm checkout with mixed or invalid "
                            + "reservation statuses, checkoutId=" + checkoutId
            );
        }

        Map<UUID, Integer> quantityByInventory = new TreeMap<>();

        for (InventoryReservation reservation : reservations) {
            if (reservation.getInventoryId() == null
                    || reservation.getQuantity() == null
                    || reservation.getQuantity() <= 0) {

                throw new IllegalStateException("Invalid inventory reservation: " + reservation.getId());
            }

            quantityByInventory.merge(
                    reservation.getInventoryId(),
                    reservation.getQuantity(),
                    Math::addExact
            );
        }

        for (UUID inventoryId : quantityByInventory.keySet()) {

            Inventory inventory = inventoryRepo.findByIdForUpdate(inventoryId);

            if (inventory == null) {
                throw new IllegalStateException("Inventory not found: " + inventoryId);
            }
        }

        for (Map.Entry<UUID, Integer> entry : quantityByInventory.entrySet()) {
            int updated = inventoryRepo.confirmReservedStock(
                    entry.getKey(),
                    entry.getValue()
            );

            if (updated != 1) {
                throw new IllegalStateException(
                        "Cannot confirm inventoryId="
                                + entry.getKey()
                );
            }
        }

        for (InventoryReservation reservation : reservations) {
            int updated = reservationRepo.markConfirmed(reservation.getId());
            if (updated != 1) {
                throw new IllegalStateException("Cannot confirm reservationId=" + reservation.getId());
            }
        }

        // 9. Ghi inventory_movements nếu model hiện tại
        //    của bạn đang ghi movement cho nghiệp vụ confirm.
        //    Các INSERT này phải cùng transaction.

        InventoryConfirmedPayload response = new InventoryConfirmedPayload(checkoutId);

        outboxService.enqueueEvent(
                "CHECKOUT",
                checkoutId,
                RESPONSE_EVENT_TYPE,
                RESPONSE_TOPIC,
                response
        );

        log.info(
                "Inventory confirmed successfully checkoutId={}, itemCount={}",
                checkoutId,
                reservations.size()
        );
    }

    private InventoryConfirmationRequestedPayload readPayload(JsonNode data) {
        try {
            return objectMapper.treeToValue(
                    data,
                    InventoryConfirmationRequestedPayload.class
            );

        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(
                    "Invalid confirmation payload",
                    ex
            );
        }
    }
}