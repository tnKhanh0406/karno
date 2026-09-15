package com.karnoproduct.entity;

import com.karnoproduct.enums.ImageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {
    private UUID id;
    private ImageType imageType;
    private String imageUrl;
    private UUID productId;
    private UUID variantId;
}