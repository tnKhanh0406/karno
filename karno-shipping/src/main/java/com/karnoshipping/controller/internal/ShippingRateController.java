package com.karnoshipping.controller.internal;

import com.karnoshipping.dto.request.ShippingRateRequest;
import com.karnoshipping.dto.response.ShippingRateResponse;
import com.karnoshipping.service.ShippingRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/shipping/rates")
@RequiredArgsConstructor
public class ShippingRateController {

    private final ShippingRateService shippingRateService;

    @PostMapping("/calculate")
    public ShippingRateResponse calculate(@RequestBody ShippingRateRequest request) {
        return shippingRateService.calculate(request);
    }
}
