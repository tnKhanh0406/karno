package com.karnoorder.messaging.payload;

import java.util.UUID;

public record InventoryConfirmationRequestedPayload(
        UUID checkoutId
) {
}