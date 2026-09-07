package com.karnoproduct.controller;

import com.karnoproduct.dto.request.ProductSearchRequest;
import com.karnoproduct.dto.response.PageResponse;
import com.karnoproduct.dto.response.ProductListItemResponse;
import com.karnoproduct.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public Mono<PageResponse<ProductListItemResponse>> getProducts(@ModelAttribute ProductSearchRequest request) {
        return productService.getProductList(request);
    }
}
