package com.karnoproduct.dto.request;

import com.karnoproduct.enums.ProductSort;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSearchRequest(
        String keyword,
        UUID categoryId,
        UUID shopId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minRating,
        ProductSort sort,
        Integer page,
        Integer size
) {
}
