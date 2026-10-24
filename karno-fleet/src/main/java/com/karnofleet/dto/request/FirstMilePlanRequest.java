package com.karnofleet.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FirstMilePlanRequest(
        @NotNull UUID vehicleId,
        @NotNull BigDecimal latitude,
        @NotNull BigDecimal longitude,
        @NotNull LocalDateTime plannedStartAt,
        @NotNull LocalDateTime desiredReturnAt
) {}
