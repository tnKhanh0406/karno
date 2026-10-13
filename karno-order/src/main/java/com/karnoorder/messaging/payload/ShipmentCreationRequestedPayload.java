package com.karnoorder.messaging.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentCreationRequestedPayload(
        UUID checkoutId,
        UUID orderId,
        UUID customerId,
        UUID shopId,
        String shippingMethod,
        BigDecimal shippingFee,
        Integer totalWeightGram,
        Instant estimatedDeliveryAt,
        Pickup pickup,
        Receiver receiver,
        List<Item> items
) {

    public record Pickup(
            String name,
            String phone,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {}

    public record Receiver(
            String name,
            String phone,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {}

    public record Item(
            UUID orderItemId,
            UUID variantId,
            Integer quantity,
            Integer weightGram,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm
    ) {}
}
