package com.karnoorder.messaging.payload;

import java.time.Instant;
import java.util.UUID;

public record OrderPackedPayload(
        UUID orderId,
        UUID shopId,
        UUID sellerId,
        Instant packedAt
) {
}
