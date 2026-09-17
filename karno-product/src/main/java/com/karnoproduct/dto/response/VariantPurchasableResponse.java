package com.karnoproduct.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record VariantPurchasableResponse(
        UUID variantId,
        String primaryImg,
        BigDecimal price,
        UUID productId,
        String productName,
        String thumbnailImg,
        UUID shopId,
        boolean isPurchasable
) {
}
