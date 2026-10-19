package com.karnofleet.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Demo planning input; does not replace the order-service entity. */
public record LastMileOrder(
        String orderId,
        DeliveryMethod deliveryMethod,
        DeliveryWindow deliveryWindow,
        GeoPoint location,
        BigDecimal weightKg,
        BigDecimal volumeM3,
        OffsetDateTime createdAt
) {
    public enum DeliveryMethod { EXPRESS, STANDARD }
    public enum DeliveryWindow { ADMIN, OUTSIDE }
}
