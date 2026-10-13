package com.karnoshipping.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PickupLocationResponse(
        UUID shopId,
        String name,
        String phone,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
