package com.karnoorder.client;

import com.karnoorder.dto.request.UUIDListRequest;
import com.karnoorder.dto.response.CheckoutAddressResponse;
import com.karnoorder.dto.response.ShopInformationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "karno-user", url = "${services.karno-user.base-url}")
public interface KarnoUserClient {
    @PostMapping("/internal/shops/batch")
    List<ShopInformationResponse> getShopsByIds(@RequestBody UUIDListRequest request);

    @GetMapping("/internal/user-addresses/{addressId}")
    CheckoutAddressResponse getUserAddressById(@PathVariable UUID addressId);
}
