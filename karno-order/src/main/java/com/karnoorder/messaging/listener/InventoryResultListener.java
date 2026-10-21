package com.karnoorder.messaging.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoorder.exception.UnrecoverableOrderCreationException;
import com.karnoorder.messaging.handler.InventoryConfirmedHandler;
import com.karnoorder.messaging.handler.InventoryResultHandler;
import com.karnoorder.messaging.model.EventEnvelope;
import com.karnoorder.messaging.payload.InventoryConfirmedPayload;
import com.karnoorder.messaging.payload.InventoryReleasedPayload;
import com.karnoorder.messaging.payload.InventoryReservationFailedPayload;
import com.karnoorder.messaging.payload.InventoryReservationExpiredPayload;
import com.karnoorder.messaging.payload.InventoryReservedPayload;
import com.karnoorder.service.InventoryCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryResultListener {
    private final InventoryResultHandler handler;
    private final ObjectMapper objectMapper;
    private final InventoryCompensationService compensationService;
    private final InventoryConfirmedHandler inventoryConfirmedHandler;

    @KafkaListener(
            topics = "${kafka.topics.inventory-reserved}",
            groupId = "karno-order-checkout-saga"
    )
    public void onInventoryReserved(String message) throws JsonProcessingException {
        log.info("Received INVENTORY_RESERVED event");

        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);

        validateEnvelope(envelope, "INVENTORY_RESERVED");

        InventoryReservedPayload payload =
                objectMapper.treeToValue(
                        envelope.data(),
                        InventoryReservedPayload.class
                );

        validateCheckoutId(
                envelope,
                payload == null
                        ? null
                        : payload.checkoutId()
        );

        try {
            handler.onReserved(envelope.eventId(), payload);
        } catch (UnrecoverableOrderCreationException ex) {
            log.error(
                    "Unrecoverable order creation error, checkoutId={}",
                    payload.checkoutId(),
                    ex
            );
            compensationService.startCompensation(
                    payload.checkoutId(),
                    "ORDER_CREATION_FAILED",
                    "Không thể tạo đơn hàng từ dữ liệu checkout đã lưu"
            );
        }

        log.info(
                "INVENTORY_RESERVED handled, checkoutId={}",
                payload.checkoutId()
        );
    }

    @KafkaListener(
            topics = "${kafka.topics.inventory-reservation-failed}",
            groupId = "karno-order-checkout-saga"
    )
    public void onInventoryReservationFailed(String message) throws JsonProcessingException {
        log.info("Received INVENTORY_RESERVATION_FAILED event");

        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);

        validateEnvelope(envelope, "INVENTORY_RESERVATION_FAILED");

        InventoryReservationFailedPayload payload =
                objectMapper.treeToValue(
                        envelope.data(),
                        InventoryReservationFailedPayload.class
                );

        validateCheckoutId(envelope, payload == null ? null : payload.checkoutId());

        handler.onReservationFailed(envelope.eventId(), payload);
        log.info(
                "INVENTORY_RESERVATION_FAILED handled, checkoutId={}",
                payload.checkoutId()
        );
    }

    @KafkaListener(
            topics = "${kafka.topics.inventory-reservation-expired}",
            groupId = "karno-order-checkout-saga"
    )
    public void onInventoryReservationExpired(String message) throws JsonProcessingException {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        validateEnvelope(envelope, "INVENTORY_RESERVATION_EXPIRED");
        InventoryReservationExpiredPayload payload = objectMapper.treeToValue(
                envelope.data(), InventoryReservationExpiredPayload.class);
        validateCheckoutId(envelope, payload == null ? null : payload.checkoutId());
        handler.onReservationExpired(envelope.eventId(), payload);
    }

    @KafkaListener(
            topics = "${kafka.topics.inventory-released}",
            groupId = "karno-order-checkout-saga"
    )
    public void onInventoryReleased(String message) throws JsonProcessingException {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);

        validateEnvelope(envelope, "INVENTORY_RELEASED");

        InventoryReleasedPayload payload =
                objectMapper.treeToValue(
                        envelope.data(),
                        InventoryReleasedPayload.class
                );

        validateCheckoutId(envelope, payload == null ? null : payload.checkoutId());

        handler.onReleased(envelope.eventId(), payload);
    }

    @KafkaListener(
            topics = "${kafka.topics.inventory-confirmed}",
            groupId = "karno-order-checkout-saga"
    )
    public void onInventoryConfirmed(
            String message
    ) throws JsonProcessingException {

        log.info(
                "[STAGE 9] Received INVENTORY_CONFIRMED"
        );

        EventEnvelope envelope =
                objectMapper.readValue(
                        message,
                        EventEnvelope.class
                );

        validateEnvelope(
                envelope,
                "INVENTORY_CONFIRMED"
        );

        InventoryConfirmedPayload payload =
                objectMapper.treeToValue(
                        envelope.data(),
                        InventoryConfirmedPayload.class
                );

        validateCheckoutId(
                envelope,
                payload == null
                        ? null
                        : payload.checkoutId()
        );

        inventoryConfirmedHandler.handle(
                envelope.eventId(),
                payload
        );

        log.info(
                "[STAGE 9] INVENTORY_CONFIRMED handled checkoutId={}",
                payload.checkoutId()
        );
    }

    private void validateEnvelope(EventEnvelope envelope, String expectedType) {
        if (envelope == null
                || envelope.eventId() == null
                || envelope.aggregateId() == null
                || envelope.data() == null
                || !expectedType.equals(envelope.eventType())) {
            throw new IllegalArgumentException("Invalid Kafka EventEnvelope, expected: " + expectedType);
        }
    }

    private void validateCheckoutId(EventEnvelope envelope, UUID checkoutId) {
        if (checkoutId == null) {
            throw new IllegalArgumentException("Missing checkoutId in Inventory event");
        }

        if (!checkoutId.equals(envelope.aggregateId())) {
            throw new IllegalArgumentException("Event aggregateId and checkoutId mismatch");
        }
    }
}
