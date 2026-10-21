package com.karnoinventory.messaging.payload;

import java.util.UUID;

public record InventoryReleaseRequestedPayload(UUID checkoutId, String reason) {
}
