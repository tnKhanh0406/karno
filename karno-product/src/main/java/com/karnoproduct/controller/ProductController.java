package com.karnoproduct.controller;

import com.karnoproduct.dto.request.ProductSearchRequest;
import com.karnoproduct.dto.response.PageResponse;
import com.karnoproduct.dto.response.ProductDetailResponse;
import com.karnoproduct.dto.response.ProductListItemResponse;
import com.karnoproduct.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public PageResponse<ProductListItemResponse> getProducts(@ModelAttribute ProductSearchRequest request) {
        return productService.getProductList(request);
    }

    @GetMapping("/{productId}")
    public ProductDetailResponse getProduct(@PathVariable String productId) {
        return productService.getProductDetail(UUID.fromString(productId));
    }
}
