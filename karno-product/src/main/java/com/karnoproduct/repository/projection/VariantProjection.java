package com.karnoproduct.repository.projection;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class VariantProjection {
    private UUID id;
    private UUID productId;
    private String sku;
    private String name;
    private BigDecimal price;
    private String primaryImg;
}
