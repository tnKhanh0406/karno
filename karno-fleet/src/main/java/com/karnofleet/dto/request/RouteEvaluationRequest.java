package com.karnofleet.dto.request;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record RouteEvaluationRequest(
        GeoPoint startLocation,
        LocalDateTime plannedStartAt,
        GeoPoint hubLocation,
        LocalDateTime shiftEndAt,
        BigDecimal capacityWeight,
        BigDecimal capacityVolume,
        BigDecimal initialWeight,
        BigDecimal initialVolume,
        List<PickupStopInput> stops
) {
}
