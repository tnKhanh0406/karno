package com.karnoorder.dto.response;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        Integer totalItems,
        List<CartShopResponse> shops
) {
}
