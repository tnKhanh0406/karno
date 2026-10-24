package com.karnoorder.dto.response;

import com.karnoorder.enums.OrderStatus;

import java.util.UUID;

public record PackOrderResponse(
        UUID orderId,
        OrderStatus status,
        boolean alreadyPacked
) {
}
