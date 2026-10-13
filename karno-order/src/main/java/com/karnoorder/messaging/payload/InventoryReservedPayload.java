package com.karnoorder.messaging.payload;

import java.util.List;
import java.util.UUID;

public record InventoryReservedPayload(
        UUID checkoutId,
        List<Item> items

) {
    public record Item(
            UUID reservationId,
            UUID orderId,
            UUID orderItemId,
            UUID variantId,
            Integer quantity
    ) {}
}
