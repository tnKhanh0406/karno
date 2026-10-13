package com.karnoorder.messaging.payload;

import java.util.UUID;

public record InventoryReleaseRequestedPayload(
        UUID checkoutId,
        String reason
) {
}
