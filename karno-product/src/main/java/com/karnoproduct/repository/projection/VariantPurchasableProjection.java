package com.karnoproduct.repository.projection;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class VariantPurchasableProjection {
    private UUID variantId;
    private String primaryImg;
    private BigDecimal price;
    private UUID productId;
    private String productName;
    private String thumbnailImg;
    private UUID shopId;
    private String productStatus;
}
