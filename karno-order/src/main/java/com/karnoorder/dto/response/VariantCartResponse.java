package com.karnoorder.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VariantCartResponse(
        UUID id,
        String name,
        String primaryImg,
        BigDecimal price,

        UUID productId,
        String productName,
        String thumbnailImg,
        UUID shopId,
        String status,

        List<VariantAttributeResponse> attributeValues
) {
}
