package com.karnofleet.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ShopPickupContext(
        UUID shopId, UUID ownerId, UUID originHubId, UUID pickupZoneId, String shopName,
        String pickupPhone, String pickupAddress,
        BigDecimal pickupLatitude, BigDecimal pickupLongitude
) {
}
