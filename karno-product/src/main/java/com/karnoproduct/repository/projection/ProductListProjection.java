package com.karnoproduct.repository.projection;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductListProjection {
    private UUID id;
    private String name;
    private String thumbnailImg;
    private BigDecimal minPrice;
    private BigDecimal rating;
    private Integer soldCount;
}
