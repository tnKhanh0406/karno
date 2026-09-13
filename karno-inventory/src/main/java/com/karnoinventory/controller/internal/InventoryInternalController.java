package com.karnoinventory.controller.internal;

import com.karnoinventory.dto.request.InventoryAvailabilityRequest;
import com.karnoinventory.dto.response.VariantInventoryResponse;
import com.karnoinventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/inventories")
public class InventoryInternalController {
    private final InventoryService inventoryService;

    @PostMapping("/availability")
    public List<VariantInventoryResponse> getAvailability(@RequestBody InventoryAvailabilityRequest request) {
        return inventoryService.getAvailabilityByVariantIds(request);
    }
}
