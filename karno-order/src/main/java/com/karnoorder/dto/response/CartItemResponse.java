package com.karnoorder.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartItemResponse(
        UUID cartItemId,

        UUID productId,
        String productName,
        String thumbnailUrl,

        UUID variantId,
        String variantName,
        String primaryImg,
        BigDecimal price,
        Integer quantity,
        Integer availableStock,
        List<VariantAttributeResponse> attributeValues,

        boolean available,
        String unavailableReason
) {
}
