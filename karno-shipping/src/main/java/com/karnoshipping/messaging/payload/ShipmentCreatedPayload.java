package com.karnoshipping.messaging.payload;

import java.time.Instant;
import java.util.UUID;

public record ShipmentCreatedPayload(UUID shipmentId, UUID orderId, UUID checkoutId,
                                     String trackingCode, String status,
                                     Instant estimatedDeliveryAt) {
}
