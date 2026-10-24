package com.karnoshipping.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PickupShipmentAssignmentRequest(
        @NotNull UUID shopId,
        @NotEmpty List<@NotNull UUID> shipmentIds
) {
}
