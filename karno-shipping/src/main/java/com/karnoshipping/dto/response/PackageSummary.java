package com.karnoshipping.dto.response;

import com.karnoshipping.dto.request.ShippingRateRequest;

import java.util.List;

public record PackageSummary(
        Integer totalWeightGram,
        List<ShippingRateRequest.Item> items
) {
}
