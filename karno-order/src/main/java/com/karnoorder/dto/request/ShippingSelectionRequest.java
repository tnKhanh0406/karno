package com.karnoorder.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ShippingSelectionRequest(
        @NotNull
        UUID shopId,

        @NotBlank
        String shippingMethod,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal displayedShippingFee,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal displayedSubtotal
) {}