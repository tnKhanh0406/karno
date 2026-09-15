package com.karnoorder.dto.response;

import java.util.UUID;

public record VariantAttributeResponse(
        UUID attributeId,
        String attributeName,
        UUID attributeValueId,
        String attributeValueName
) {
}
