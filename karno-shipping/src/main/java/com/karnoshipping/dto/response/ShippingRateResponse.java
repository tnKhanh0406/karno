package com.karnoshipping.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShippingRateResponse(
        List<Rate> rates
) {

    public record Rate(
            UUID shopId,
            String shippingMethod,
            BigDecimal shippingFee,
            Integer totalWeightGram,
            Instant estimatedDeliveryAt,
            PickupSnapshot pickup
    ) {
    }

    public record PickupSnapshot(
            String name,
            String phone,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }
}
