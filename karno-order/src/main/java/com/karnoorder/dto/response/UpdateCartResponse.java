package com.karnoorder.dto.response;

import java.util.UUID;

public record UpdateCartResponse(
        UUID cartItemId,
        Integer quantity
) {
}
