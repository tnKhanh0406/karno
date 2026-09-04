package com.karnoproduct.entity;

import com.karnoproduct.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private UUID id;
    private UUID shopId;
    private UUID categoryId;
    private String name;
    private String description;
    private ProductStatus status;
    private Integer soldCount;
    private BigDecimal rating;
    private String thumbnailImg;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}