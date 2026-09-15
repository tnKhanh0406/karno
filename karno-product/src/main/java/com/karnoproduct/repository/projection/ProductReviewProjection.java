package com.karnoproduct.repository.projection;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ProductReviewProjection {
    private UUID id;
    private UUID userId;
    private String comment;
    private Integer rating;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
