package com.karnoshipping.dto.response;

import com.karnoshipping.entity.Shipment;
import com.karnoshipping.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReadyShipmentResponse(
        UUID shipmentId,
        UUID orderId,
        UUID shopId,
        ShipmentStatus status,
        String trackingCode,
        String shippingMethod,
        Integer totalWeightGram,
        String pickupName,
        String pickupPhone,
        String pickupAddress,
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReadyShipmentResponse from(Shipment shipment) {
        return new ReadyShipmentResponse(
                shipment.getId(), shipment.getOrderId(), shipment.getShopId(),
                shipment.getStatus(), shipment.getTrackingCode(), shipment.getShippingMethod(),
                shipment.getTotalWeightGram(), shipment.getPickupName(), shipment.getPickupPhone(),
                shipment.getPickupAddress(), shipment.getPickupLatitude(),
                shipment.getPickupLongitude(), shipment.getCreatedAt(), shipment.getUpdatedAt());
    }
}
