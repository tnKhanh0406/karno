package com.karnoshipping.client;

import com.karnoshipping.dto.response.PickupLocationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@FeignClient(name = "karno-user", url = "${services.karno-user.base-url}")
public interface KarnoUserClient {

    @PostMapping("/internal/shops/pickup-locations/batch")
    List<PickupLocationResponse> getPickupLocations(@RequestBody Set<UUID> shopIds);
}
