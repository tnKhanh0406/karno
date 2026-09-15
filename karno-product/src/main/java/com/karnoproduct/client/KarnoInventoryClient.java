package com.karnoproduct.client;

import com.karnoproduct.dto.request.InventoryAvailabilityRequest;
import com.karnoproduct.dto.response.VariantInventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "karno-inventory", url = "${services.karno-inventory.base-url}")
public interface KarnoInventoryClient {
    @PostMapping("/internal/inventories/availability")
    List<VariantInventoryResponse> getAvailabilityByVariantIds(@RequestBody InventoryAvailabilityRequest request);
}
