package com.karnoproduct.repository.projection;

import lombok.Data;

import java.util.UUID;

@Data
public class AttributeValueProjection {
    private UUID attributeId;
    private String attributeName;
    private UUID valueId;
    private String value;
}
