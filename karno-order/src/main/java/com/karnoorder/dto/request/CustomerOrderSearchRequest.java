package com.karnoorder.dto.request;

import com.karnoorder.enums.OrderStatus;

public record CustomerOrderSearchRequest(
        OrderStatus status,
        String keyword,
        Integer page,
        Integer size
) {
}
