package com.karnoproduct.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantAttributeValue {
    private UUID id;
    private UUID attributeValueId;
    private UUID variantId;
}
