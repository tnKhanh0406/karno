package com.karnoproduct.service;

import com.karnoproduct.dto.request.ProductSearchRequest;
import com.karnoproduct.dto.response.PageResponse;
import com.karnoproduct.dto.response.ProductListItemResponse;
import reactor.core.publisher.Mono;

public interface ProductService {
    Mono<PageResponse<ProductListItemResponse>> getProductList(ProductSearchRequest request);
}
