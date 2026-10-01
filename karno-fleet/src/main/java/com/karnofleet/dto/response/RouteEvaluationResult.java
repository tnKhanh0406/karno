package com.karnofleet.dto.response;

import com.karnofleet.enums.RouteViolation;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record RouteEvaluationResult(
        boolean feasible,
        List<EvaluatedStop> stops,
        LocalDateTime returnToHubAt,
        BigDecimal finalWeight,
        BigDecimal finalVolume,
        List<RouteViolation> violations
) {
}
