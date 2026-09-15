package com.karnoorder.dto.response;

import java.util.UUID;

public record VariantInventoryResponse(
        UUID variantId,
        Integer availableQuantity
) {
}
