package com.karnofleet.dto.response;

import com.karnofleet.enums.RouteViolation;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record RouteSummaryResponse(
        boolean feasible,
        int totalStops,
        BigDecimal finalWeight,
        BigDecimal finalVolume,
        LocalDateTime returnToHubAt,
        List<RouteViolation> violations
) {}
