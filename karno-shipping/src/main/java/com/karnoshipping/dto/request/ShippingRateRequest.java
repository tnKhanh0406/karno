package com.karnoshipping.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ShippingRateRequest(
        ShippingDropoff dropoff,
        List<ShipmentRequest> shipments

) {
    public record ShippingDropoff(
            UUID addressId,
            String receiverName,
            String receiverPhone,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }

    public record ShipmentRequest(
            UUID shopId,
            String shippingMethod,
            List<Item> items
    ) {
    }

    public record Item(
            UUID orderItemId,
            UUID variantId,
            Integer quantity,
            Integer weightGram,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm
    ) {
    }
}
