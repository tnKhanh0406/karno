package com.karnoshipping.service;

import com.karnoshipping.dto.request.ShippingRateRequest;
import com.karnoshipping.dto.response.ShippingRateResponse;

public interface ShippingRateService {
    ShippingRateResponse calculate(ShippingRateRequest request);
}
