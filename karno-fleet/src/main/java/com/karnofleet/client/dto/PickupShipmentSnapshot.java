package com.karnofleet.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PickupShipmentSnapshot(
        UUID shipmentId, UUID orderId, UUID shopId, String trackingCode,
        Integer weightGram, BigDecimal volumeM3, String pickupName,
        String pickupPhone, String pickupAddress,
        BigDecimal pickupLatitude, BigDecimal pickupLongitude
) {
}
