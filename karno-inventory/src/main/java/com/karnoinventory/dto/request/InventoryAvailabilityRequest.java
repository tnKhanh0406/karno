package com.karnoinventory.dto.request;

import java.util.List;
import java.util.UUID;

public record InventoryAvailabilityRequest(
        List<UUID> variantIds
) {
}
