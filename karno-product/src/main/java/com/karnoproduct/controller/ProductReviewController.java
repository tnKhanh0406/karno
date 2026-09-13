package com.karnoproduct.controller;

import com.karnoproduct.dto.response.PageResponse;
import com.karnoproduct.dto.response.ProductReviewResponse;
import com.karnoproduct.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product-reviews")
public class ProductReviewController {
    private final ProductReviewService productReviewService;

    @GetMapping("/{productId}")
    public PageResponse<ProductReviewResponse> getReviewsByProductId(@PathVariable String productId,
                                                                     @RequestParam(required = false) Integer page,
                                                                     @RequestParam(required = false) Integer size,
                                                                     @RequestParam(required = false) Integer rating) {
        return productReviewService.getReviewsByProductId(
                UUID.fromString(productId),
                page,
                size,
                rating
        );
    }
}
