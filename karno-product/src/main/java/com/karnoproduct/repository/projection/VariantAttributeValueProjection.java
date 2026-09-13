package com.karnoproduct.repository.projection;

import lombok.Data;

import java.util.UUID;

@Data
public class VariantAttributeValueProjection {
    private UUID variantId;
    private UUID attributeValueId;
}
