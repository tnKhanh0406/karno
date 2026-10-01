package com.karnofleet.dto.response;

import com.karnofleet.dto.request.GeoPoint;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record EvaluatedStop(
        int sequence,
        UUID requestId,
        String requestCode,
        GeoPoint location,
        LocalDateTime arrivalAt,
        LocalDateTime windowStartAt,
        LocalDateTime windowEndAt,
        LocalDateTime serviceStartAt,
        LocalDateTime departureAt,
        Duration waitingTime,
        BigDecimal loadWeightAfter,
        BigDecimal loadVolumeAfter
) {
}
