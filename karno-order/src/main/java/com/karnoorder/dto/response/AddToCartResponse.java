package com.karnoorder.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AddToCartResponse(
        UUID productId,
        String productName,
        String thumbnailImg,
        UUID variantId,
        BigDecimal price,
        String primaryImg,
        Integer quantity
) {
}
