package com.karnoproduct.service;

import com.karnoproduct.dto.request.ProductSearchRequest;
import com.karnoproduct.dto.response.PageResponse;
import com.karnoproduct.dto.response.ProductListItemResponse;

public interface ProductService {
    PageResponse<ProductListItemResponse> getProductList(ProductSearchRequest request);
}
