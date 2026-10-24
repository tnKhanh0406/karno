package com.karnoshipping.messaging.payload;

import java.time.Instant;
import java.util.UUID;

public record ShipmentReadyForPickupPayload(
        UUID shipmentId,
        UUID orderId,
        UUID shopId,
        Instant readyAt
) {
}
