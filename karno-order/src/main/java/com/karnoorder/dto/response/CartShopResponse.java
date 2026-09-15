package com.karnoorder.dto.response;

import java.util.List;
import java.util.UUID;

public record CartShopResponse(
        UUID shopId,
        String shopName,
        String shopLogo,
        List<CartItemResponse> items
) {
}
