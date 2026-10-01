package com.karnofleet.dto.request;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Builder
public record PickupStopInput(
        UUID requestId,
        String requestCode,
        GeoPoint location,
        BigDecimal weight,
        BigDecimal volume,
        LocalTime windowStart,
        LocalTime windowEnd,
        int serviceDurationSeconds,
        LocalDateTime createdAt
) {
}
