package com.karnoorder.client;

import com.karnoorder.dto.request.UUIDListRequest;
import com.karnoorder.dto.response.VariantCartResponse;
import com.karnoorder.dto.response.VariantCheckoutInfo;
import com.karnoorder.dto.response.VariantPurchasableResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "karno-product", url = "${services.karno-product.base-url}")
public interface KarnoProductClient {
    @PostMapping("/internal/product-variants/cart-info")
    List<VariantCartResponse> getVariantCartInfo(@RequestBody UUIDListRequest request);

    @GetMapping("/internal/product-variants/{variantId}/purchasable-info")
    VariantPurchasableResponse getVariantPurchasableInfo(@PathVariable UUID variantId);

    @PostMapping("/internal/product-variants/checkout-info")
    List<VariantCheckoutInfo> getCheckoutInfo(@RequestBody UUIDListRequest request);
}
