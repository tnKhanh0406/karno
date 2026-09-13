package com.karnoproduct.service;

import com.karnoproduct.dto.request.ProductSearchRequest;
import com.karnoproduct.dto.response.PageResponse;
import com.karnoproduct.dto.response.ProductDetailResponse;
import com.karnoproduct.dto.response.ProductListItemResponse;

import java.util.UUID;

public interface ProductService {
    PageResponse<ProductListItemResponse> getProductList(ProductSearchRequest request);
    ProductDetailResponse getProductDetail(UUID productId);
}
