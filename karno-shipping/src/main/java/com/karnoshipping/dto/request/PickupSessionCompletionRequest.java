package com.karnoshipping.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PickupSessionCompletionRequest(
        @NotNull List<UUID> pickedUpPickupRequestIds,
        @NotNull List<UUID> unpickedPickupRequestIds
) {}
