package com.karnoshipping.dto.response;

import com.karnoshipping.entity.Shipment;

import java.math.BigDecimal;
import java.util.UUID;

public record PickupShipmentSnapshot(
        UUID shipmentId,
        UUID orderId,
        UUID shopId,
        String trackingCode,
        Integer weightGram,
        BigDecimal volumeM3,
        String pickupName,
        String pickupPhone,
        String pickupAddress,
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude
) {
    public static PickupShipmentSnapshot from(Shipment shipment) {
        return new PickupShipmentSnapshot(
                shipment.getId(), shipment.getOrderId(), shipment.getShopId(),
                shipment.getTrackingCode(), shipment.getTotalWeightGram(),
                shipment.getTotalVolumeM3(), shipment.getPickupName(),
                shipment.getPickupPhone(), shipment.getPickupAddress(),
                shipment.getPickupLatitude(), shipment.getPickupLongitude());
    }
}
