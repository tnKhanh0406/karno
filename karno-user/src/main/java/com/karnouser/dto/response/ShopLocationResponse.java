package com.karnouser.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ShopLocationResponse(
        UUID shopId,
        String name,
        String phone,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
