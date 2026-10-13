package com.karnoorder.dto.request;

import com.karnoorder.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CreateOrderRequest(
        @NotEmpty
        Set<@NotNull UUID> cartItemIds,
        @NotNull
        UUID addressId,
        @NotEmpty
        List<@Valid ShippingSelectionRequest> shippingSelections,
        @NotNull
        PaymentMethod paymentMethod,
        String note,
        Instant deliverySlotStart,
        Instant deliverySlotEnd
) {}
