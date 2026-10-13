package com.karnoorder.dto.response;

import com.karnoorder.enums.CheckoutSagaStatus;

import java.util.UUID;

public record CheckoutStartResponse(
        UUID checkoutId,
        CheckoutSagaStatus status,
        boolean reused,
        String message
) {}
