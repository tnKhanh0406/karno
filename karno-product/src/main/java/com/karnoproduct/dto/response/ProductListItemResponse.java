package com.karnoproduct.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductListItemResponse(
        UUID id,
        String name,
        String thumbnailImg,
        BigDecimal minPrice,
        BigDecimal rating,
        Integer soldCount
) {
}
