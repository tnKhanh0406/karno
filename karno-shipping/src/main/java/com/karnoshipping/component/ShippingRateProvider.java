package com.karnoshipping.component;

import com.karnoshipping.dto.request.ShippingRateRequest;
import com.karnoshipping.dto.response.PackageSummary;
import com.karnoshipping.dto.response.PickupLocationResponse;
import com.karnoshipping.dto.response.ShippingRate;

public interface ShippingRateProvider {
    ShippingRate calculate(
            PickupLocationResponse pickup,
            ShippingRateRequest.ShippingDropoff dropoff,
            PackageSummary packageSummary,
            String shippingMethod
    );
}
