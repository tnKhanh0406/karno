package com.karnoproduct.controller.internal;

import com.karnoproduct.dto.request.UUIDListRequest;
import com.karnoproduct.dto.response.VariantCartResponse;
import com.karnoproduct.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/product-variants")
public class ProductVariantInternalController {
    private final ProductVariantService productVariantService;

    @PostMapping("/cart-info")
    public List<VariantCartResponse> getVariantCartResponses(@RequestBody UUIDListRequest request) {
        return productVariantService.getVariantCartResponses(request.UUIDs());
    }
}
