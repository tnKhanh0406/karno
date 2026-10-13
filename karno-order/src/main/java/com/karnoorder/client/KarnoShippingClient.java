package com.karnoorder.client;

import com.karnoorder.dto.request.ShippingRateRequest;
import com.karnoorder.dto.response.ShippingRateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "karno-shipping", url = "${services.karno-shipping.base-url}")
public interface KarnoShippingClient {
    @PostMapping("/internal/shipping/rates/calculate")
    ShippingRateResponse calculate(@RequestBody ShippingRateRequest request);
}