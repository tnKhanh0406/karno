package com.karnoinventory.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;
import java.util.UUID;

public record InventoryAvailabilityRequest(
        @JsonAlias("UUIDs")
        List<UUID> variantIds
) {
}
