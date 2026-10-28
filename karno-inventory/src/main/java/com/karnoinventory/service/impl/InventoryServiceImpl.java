package com.karnoinventory.service.impl;

import com.karnoinventory.dto.request.InventoryAvailabilityRequest;
import com.karnoinventory.dto.response.VariantInventoryResponse;
import com.karnoinventory.repository.InventoryRepo;
import com.karnoinventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepo inventoryRepo;

    @Override
    public List<VariantInventoryResponse> getAvailabilityByVariantIds(InventoryAvailabilityRequest request) {
        if (request == null || request.variantIds() == null || request.variantIds().isEmpty()) {
            return List.of();
        }
        return inventoryRepo.getAvailabilityByVariantIds(request.variantIds());
    }
}
