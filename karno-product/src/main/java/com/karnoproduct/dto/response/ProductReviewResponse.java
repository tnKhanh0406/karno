package com.karnoproduct.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductReviewResponse(
        UUID id,
        String comment,
        Integer rating,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,

        ReviewUserResponse user
) {
}
