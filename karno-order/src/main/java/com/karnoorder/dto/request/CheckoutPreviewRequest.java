package com.karnoorder.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record CheckoutPreviewRequest(
        @NotEmpty
        Set<@NotNull UUID> cartItemIds,
        @NotNull
        UUID addressId
) {
}
