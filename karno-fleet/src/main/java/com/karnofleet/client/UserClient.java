package com.karnofleet.client;

import com.karnofleet.client.dto.ShopPickupContext;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "fleet-user-client", url = "${services.karno-user.base-url}")
public interface UserClient {
    @GetMapping("/internal/shops/{shopId}/pickup-context")
    ShopPickupContext getPickupContext(@PathVariable UUID shopId);
}
