package com.karnoorder.dto.response;

import com.karnoorder.enums.CheckoutSagaStatus;

import java.time.Instant;
import java.util.UUID;

public record CheckoutStatusResponse(
        UUID checkoutId,
        CheckoutSagaStatus status,
        String failureCode,
        Instant completedAt
) {}
