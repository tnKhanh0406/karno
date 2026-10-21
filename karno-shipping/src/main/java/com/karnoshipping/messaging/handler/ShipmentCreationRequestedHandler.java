package com.karnoshipping.messaging.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoshipping.entity.Shipment;
import com.karnoshipping.enums.ShipmentStatus;
import com.karnoshipping.messaging.model.EventEnvelope;
import com.karnoshipping.messaging.payload.ShipmentCreatedPayload;
import com.karnoshipping.messaging.payload.ShipmentCreationRequestedPayload;
import com.karnoshipping.repository.ProcessedEventRepo;
import com.karnoshipping.repository.ShipmentRepo;
import com.karnoshipping.repository.ShipmentStatusHistoryRepo;
import com.karnoshipping.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentCreationRequestedHandler {
    private static final String REQUEST_TYPE = "SHIPMENT_CREATION_REQUESTED";
    private static final String CONSUMER_NAME = "shipment-creation-requested-handler";
    private static final String CREATED_TOPIC = "shipping.shipment.created";

    private final ProcessedEventRepo processedEventRepo;
    private final ShipmentRepo shipmentRepo;
    private final ShipmentStatusHistoryRepo historyRepo;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(EventEnvelope event) {
        validateEnvelope(event);
        ShipmentCreationRequestedPayload payload = objectMapper.convertValue(
                event.data(), ShipmentCreationRequestedPayload.class);
        validatePayload(event, payload);

        if (processedEventRepo.insertIfAbsent(event.eventId(), CONSUMER_NAME, REQUEST_TYPE) == 0) {
            return;
        }

        Shipment shipment = shipmentRepo.findByOrderId(payload.orderId());
        if (shipment == null) {
            shipment = createShipment(payload);
        } else {
            validateExisting(shipment, payload);
        }

        outboxService.enqueue(
                "SHIPMENT", shipment.getId(), "SHIPMENT_CREATED", CREATED_TOPIC,
                new ShipmentCreatedPayload(
                        shipment.getId(), shipment.getOrderId(), shipment.getCheckoutId(),
                        shipment.getTrackingCode(), shipment.getStatus().name(),
                        shipment.getEstimatedDeliveryAt())
        );
    }

    private Shipment createShipment(ShipmentCreationRequestedPayload payload) {
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = Shipment.builder()
                .id(shipmentId)
                .orderId(payload.orderId()).checkoutId(payload.checkoutId())
                .customerId(payload.customerId()).shopId(payload.shopId())
                .status(ShipmentStatus.CREATED)
                .trackingCode("KARNO-" + shipmentId.toString().replace("-", "").toUpperCase())
                .shippingMethod(payload.shippingMethod()).shippingFee(payload.shippingFee())
                .totalWeightGram(payload.totalWeightGram())
                .packagePayload(serializeItems(payload))
                .pickupName(payload.pickup().name()).pickupPhone(payload.pickup().phone())
                .pickupAddress(payload.pickup().address())
                .pickupLatitude(payload.pickup().latitude()).pickupLongitude(payload.pickup().longitude())
                .receiverName(payload.receiver().name()).receiverPhone(payload.receiver().phone())
                .receiverAddress(payload.receiver().address())
                .receiverLatitude(payload.receiver().latitude()).receiverLongitude(payload.receiver().longitude())
                .estimatedDeliveryAt(payload.estimatedDeliveryAt())
                .build();
        if (shipmentRepo.insert(shipment) != 1
                || historyRepo.insertInitial(UUID.randomUUID(), shipmentId, ShipmentStatus.CREATED.name()) != 1) {
            throw new IllegalStateException("Cannot create shipment for orderId=" + payload.orderId());
        }
        return shipment;
    }

    private String serializeItems(ShipmentCreationRequestedPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload.items());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Cannot serialize shipment package", ex);
        }
    }

    private void validateEnvelope(EventEnvelope event) {
        if (event == null || event.eventId() == null || event.aggregateId() == null
                || event.data() == null || !REQUEST_TYPE.equals(event.eventType())) {
            throw new IllegalArgumentException("Invalid shipment creation event");
        }
    }

    private void validatePayload(EventEnvelope event, ShipmentCreationRequestedPayload payload) {
        if (payload == null || payload.orderId() == null || payload.checkoutId() == null
                || payload.customerId() == null || payload.shopId() == null
                || !payload.orderId().equals(event.aggregateId())
                || payload.shippingMethod() == null || payload.shippingMethod().isBlank()
                || payload.shippingFee() == null || payload.shippingFee().signum() < 0
                || payload.totalWeightGram() == null || payload.totalWeightGram() <= 0
                || payload.pickup() == null || payload.receiver() == null
                || payload.items() == null || payload.items().isEmpty()) {
            throw new IllegalArgumentException("Invalid shipment creation payload");
        }
    }

    private void validateExisting(Shipment shipment, ShipmentCreationRequestedPayload payload) {
        if (!Objects.equals(shipment.getCheckoutId(), payload.checkoutId())
                || !Objects.equals(shipment.getCustomerId(), payload.customerId())
                || !Objects.equals(shipment.getShopId(), payload.shopId())
                || !Objects.equals(shipment.getShippingMethod(), payload.shippingMethod())
                || shipment.getShippingFee() == null
                || shipment.getShippingFee().compareTo(payload.shippingFee()) != 0
                || !Objects.equals(shipment.getTotalWeightGram(), payload.totalWeightGram())) {
            throw new IllegalStateException(
                    "Existing shipment does not match order snapshot: " + payload.orderId());
        }
    }
}
