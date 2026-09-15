package com.karnoproduct.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VariantResponse(
        UUID id,
        String sku,
        String name,
        BigDecimal price,
        String primaryImg,
        Integer availableQuantity,

        List<UUID> attributes
) {
}
