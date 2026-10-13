package com.karnoorder.dto.response;

import java.util.UUID;

public record ShopCheckoutInfo(
        UUID shopId,
        String shopName
) {
}