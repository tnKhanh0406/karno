package com.karnoshipping.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ShippingRate(
        BigDecimal fee,
        Instant estimatedDeliveryAt
) {
}