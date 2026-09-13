package com.karnoproduct.service;

import com.karnoproduct.dto.response.PageResponse;
import com.karnoproduct.dto.response.ProductReviewResponse;

import java.util.UUID;

public interface ProductReviewService {
    PageResponse<ProductReviewResponse> getReviewsByProductId(UUID productId, Integer page, Integer size, Integer rating);
}
