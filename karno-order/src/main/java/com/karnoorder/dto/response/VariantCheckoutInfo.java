package com.karnoorder.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record VariantCheckoutInfo(
        UUID variantId,
        UUID productId,
        UUID shopId,

        String productName,
        String variantName,
        String sku,
        String primaryImageUrl,

        BigDecimal price,

        Integer weightGram,

        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm,

        boolean sellable
) {
}
