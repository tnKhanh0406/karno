package com.karnoshipping.messaging.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoshipping.constants.EventTopics;
import com.karnoshipping.constants.EventTypes;
import com.karnoshipping.entity.Shipment;
import com.karnoshipping.enums.ShipmentStatus;
import com.karnoshipping.messaging.model.EventEnvelope;
import com.karnoshipping.messaging.payload.OrderPackedPayload;
import com.karnoshipping.messaging.payload.ShipmentReadyForPickupPayload;
import com.karnoshipping.repository.ProcessedEventRepo;
import com.karnoshipping.repository.ShipmentRepo;
import com.karnoshipping.repository.ShipmentStatusHistoryRepo;
import com.karnoshipping.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPackedHandler {
    private static final String CONSUMER_NAME = "order-packed-handler";

    private final ProcessedEventRepo processedEventRepo;
    private final ShipmentRepo shipmentRepo;
    private final ShipmentStatusHistoryRepo historyRepo;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(EventEnvelope event) {
        validateEnvelope(event);
        OrderPackedPayload payload = objectMapper.convertValue(event.data(), OrderPackedPayload.class);
        validatePayload(event, payload);

        if (processedEventRepo.insertIfAbsent(event.eventId(), CONSUMER_NAME,
                EventTypes.ORDER_PACKED) == 0) {
            return;
        }

        Shipment shipment = shipmentRepo.findByOrderId(payload.orderId());
        if (shipment == null) {
            throw new IllegalStateException("Shipment has not been created for orderId=" + payload.orderId());
        }
        if (!payload.shopId().equals(shipment.getShopId())) {
            throw new IllegalStateException("ORDER_PACKED shop does not match shipment shop");
        }
        if (shipment.getStatus() == ShipmentStatus.READY_FOR_PICKUP) {
            return;
        }
        if (shipment.getStatus() != ShipmentStatus.CREATED) {
            throw new IllegalStateException("Shipment cannot become READY_FOR_PICKUP from "
                    + shipment.getStatus());
        }
        if (shipmentRepo.markReadyForPickup(payload.orderId(), payload.shopId()) != 1) {
            throw new IllegalStateException("Cannot mark shipment ready for pickup");
        }
        if (historyRepo.insertTransition(
                UUID.randomUUID(), shipment.getId(), ShipmentStatus.CREATED.name(),
                ShipmentStatus.READY_FOR_PICKUP.name(), "Order was packed by seller") != 1) {
            throw new IllegalStateException("Cannot insert shipment status history");
        }

        Instant readyAt = Instant.now();
        outboxService.enqueue(
                "SHIPMENT", shipment.getId(), EventTypes.SHIPMENT_READY_FOR_PICKUP,
                EventTopics.SHIPMENT_READY_FOR_PICKUP,
                new ShipmentReadyForPickupPayload(
                        shipment.getId(), shipment.getOrderId(), shipment.getShopId(), readyAt));
    }

    private void validateEnvelope(EventEnvelope event) {
        if (event == null || event.eventId() == null || event.aggregateId() == null
                || event.data() == null || !EventTypes.ORDER_PACKED.equals(event.eventType())) {
            throw new IllegalArgumentException("Invalid ORDER_PACKED event");
        }
    }

    private void validatePayload(EventEnvelope event, OrderPackedPayload payload) {
        if (payload == null || payload.orderId() == null || payload.shopId() == null
                || payload.sellerId() == null || payload.packedAt() == null
                || !payload.orderId().equals(event.aggregateId())) {
            throw new IllegalArgumentException("Invalid ORDER_PACKED payload");
        }
    }
}
