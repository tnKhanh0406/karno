package com.karnoproduct.repository.projection;

import com.karnoproduct.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class VariantCartProjection {
    private UUID id;
    private String name;
    private String primaryImg;
    private BigDecimal price;

    private UUID productId;
    private String productName;
    private String thumbnailImg;
    private UUID shopId;
    private ProductStatus productStatus;
}
