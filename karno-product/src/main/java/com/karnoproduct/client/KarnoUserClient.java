package com.karnoproduct.client;

import com.karnoproduct.dto.request.UUIDListRequest;
import com.karnoproduct.dto.response.ReviewUserResponse;
import com.karnoproduct.dto.response.ShopInformationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "karno-user", url = "${services.karno-user.base-url}")
public interface KarnoUserClient {
    @GetMapping("/internal/shops/{shopId}")
    ShopInformationResponse getShopById(@PathVariable("shopId") UUID shopId);

    @PostMapping("/internal/users/batch")
    List<ReviewUserResponse> getUsersByIds(@RequestBody UUIDListRequest request);
}
