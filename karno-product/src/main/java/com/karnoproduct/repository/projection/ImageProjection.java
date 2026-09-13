package com.karnoproduct.repository.projection;

import com.karnoproduct.enums.ImageType;
import lombok.Data;

import java.util.UUID;

@Data
public class ImageProjection {
    private UUID id;
    private ImageType imageType;
    private String imageUrl;
    private UUID productId;
    private UUID variantId;
}
