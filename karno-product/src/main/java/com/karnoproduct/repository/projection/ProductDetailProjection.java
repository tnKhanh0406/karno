package com.karnoproduct.repository.projection;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductDetailProjection {
    private UUID id;
    private UUID shopId;
    private UUID categoryId;
    private String name;
    private String description;
    private String thumbnailImg;
    private Integer soldCount;
    private BigDecimal rating;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
