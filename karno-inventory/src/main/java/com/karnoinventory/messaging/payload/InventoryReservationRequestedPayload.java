package com.karnoinventory.messaging.payload;

import java.util.List;
import java.util.UUID;

public record InventoryReservationRequestedPayload(
        UUID checkoutId,
        List<Item> items
) {
    public record Item(
            UUID orderId,
            UUID orderItemId,
            UUID variantId,
            Integer quantity
    ) {}
}
