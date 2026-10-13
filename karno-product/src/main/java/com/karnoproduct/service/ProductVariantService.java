package com.karnoproduct.service;

import com.karnoproduct.dto.response.VariantCartResponse;
import com.karnoproduct.dto.response.VariantCheckoutInfo;
import com.karnoproduct.dto.response.VariantPurchasableResponse;

import java.util.List;
import java.util.UUID;

public interface ProductVariantService {
    List<VariantCartResponse> getVariantCartResponses(List<UUID> variantIds);
    VariantPurchasableResponse getVariantPurchasableInfo(UUID variantId);
    List<VariantCheckoutInfo> getVariantCheckoutInfo(List<UUID> variantIds);
}
