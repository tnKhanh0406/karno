package com.karnoinventory.messaging.model;

import java.util.UUID;

public record ReservationValidationFailure(
        String code,
        UUID variantId,
        Integer requestedQuantity,
        Integer availableQuantity,
        String message
) {
}
