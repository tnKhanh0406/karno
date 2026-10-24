package com.karnouser.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ShopPickupContextResponse(
        UUID shopId,
        UUID ownerId,
        UUID originHubId,
        UUID pickupZoneId,
        String shopName,
        String pickupPhone,
        String pickupAddress,
        BigDecimal pickupLatitude,
        BigDecimal pickupLongitude
) {
}
