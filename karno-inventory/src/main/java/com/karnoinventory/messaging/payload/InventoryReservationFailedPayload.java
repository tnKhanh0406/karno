package com.karnoinventory.messaging.payload;

import java.util.UUID;

public record InventoryReservationFailedPayload(
        UUID checkoutId,
        String code,
        String message,
        UUID variantId,
        Integer requestedQuantity,
        Integer availableQuantity
) {
}
