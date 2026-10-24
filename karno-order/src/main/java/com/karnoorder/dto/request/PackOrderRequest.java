package com.karnoorder.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PackOrderRequest(
        @NotNull UUID shopId,
        @NotNull UUID sellerId
) {
}
