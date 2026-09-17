package com.karnoorder.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToCartRequest(
        @NotNull(message = "Variant ID is required")
        UUID variantId,
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be greater than 0")
        Integer quantity
) {
}
