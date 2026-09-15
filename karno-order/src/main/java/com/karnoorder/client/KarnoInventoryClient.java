package com.karnoorder.client;

import com.karnoorder.dto.request.UUIDListRequest;
import com.karnoorder.dto.response.VariantInventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "karno-inventory", url = "${services.karno-inventory.base-url}")
public interface KarnoInventoryClient {
    @PostMapping("/internal/inventories/availability")
    List<VariantInventoryResponse> getAvailabilityByVariantIds(@RequestBody UUIDListRequest request);
}
