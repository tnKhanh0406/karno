package com.karnoproduct.repository.projection;

import lombok.Data;

import java.util.UUID;

@Data
public class VariantAttributeProjection {
    private UUID variantId;

    private UUID attributeId;
    private String attributeName;

    private  UUID attributeValueId;
    private String attributeValueName;
}
