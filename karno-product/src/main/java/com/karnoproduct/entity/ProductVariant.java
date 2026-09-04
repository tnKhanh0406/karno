package com.karnoproduct.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    private UUID id;
    private UUID productId;
    private String sku;
    private String name;
    private BigDecimal price;
    private String primaryImg;
    private Integer weightGram;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
}
