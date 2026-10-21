package com.karnoorder.messaging.payload;

import java.util.UUID;

public record InventoryReservationExpiredPayload(UUID checkoutId) {
}
