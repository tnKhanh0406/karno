package com.karnoinventory.service;

import com.karnoinventory.dto.request.InventoryAvailabilityRequest;
import com.karnoinventory.dto.response.VariantInventoryResponse;

import java.util.List;

public interface InventoryService {
    List<VariantInventoryResponse> getAvailabilityByVariantIds(InventoryAvailabilityRequest request);
}
