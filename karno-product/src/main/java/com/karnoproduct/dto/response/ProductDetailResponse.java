package com.karnoproduct.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id,
        String name,
        String description,
        String thumbnailImg,
        Integer soldCount,
        BigDecimal rating,
        BigDecimal minPrice,
        BigDecimal maxPrice,

        List<CategoryResponse> categoryBreadcrumbs,

        List<ImageResponse> images,

        List<VariantResponse> variants,

        List<ProductAttributeResponse> attributes,

        ShopInformationResponse shopInformation,

        RatingSummaryResponse ratingSummary
) {
}
