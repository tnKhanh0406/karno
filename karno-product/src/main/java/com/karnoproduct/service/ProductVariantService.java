package com.karnoproduct.service;

import com.karnoproduct.dto.response.VariantCartResponse;

import java.util.List;
import java.util.UUID;

public interface ProductVariantService {
    List<VariantCartResponse> getVariantCartResponses(List<UUID> variantIds);
}
