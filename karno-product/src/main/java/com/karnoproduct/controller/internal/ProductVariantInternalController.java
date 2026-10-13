package com.karnoproduct.controller.internal;

import com.karnoproduct.dto.request.UUIDListRequest;
import com.karnoproduct.dto.response.VariantCartResponse;
import com.karnoproduct.dto.response.VariantCheckoutInfo;
import com.karnoproduct.dto.response.VariantPurchasableResponse;
import com.karnoproduct.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/product-variants")
public class ProductVariantInternalController {
    private final ProductVariantService productVariantService;

    @PostMapping("/cart-info")
    public List<VariantCartResponse> getVariantCartResponses(@RequestBody UUIDListRequest request) {
        return productVariantService.getVariantCartResponses(request.UUIDs());
    }

    @GetMapping("/{variantId}/purchasable-info")
    public VariantPurchasableResponse getVariantPurchasableInfo(@PathVariable UUID variantId) {
        return productVariantService.getVariantPurchasableInfo(variantId);
    }

    @PostMapping("/checkout-info")
    public List<VariantCheckoutInfo> getVariantCheckoutInfo(@RequestBody UUIDListRequest request) {
        return productVariantService.getVariantCheckoutInfo(request.UUIDs());
    }
}
