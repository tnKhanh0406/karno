package com.karnofleet.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreatePickupRequest(
        @NotNull UUID shopId,
        @NotNull UUID sellerId,
        @NotEmpty List<@NotNull UUID> shipmentIds,
        @NotNull LocalTime pickupWindowStart,
        @NotNull LocalTime pickupWindowEnd,
        @Min(0) Integer serviceDurationSeconds
) {
}
