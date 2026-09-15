package com.karnoproduct.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReview {
    private UUID id;
    private String comment;
    private Integer rating;
    private UUID productId;
    private UUID shopId;
    private UUID userId;
    private UUID orderItemId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
