package com.karnoproduct.dto.response;

import java.util.List;

public record RatingSummaryResponse(
        Long totalReviews,
        List<RatingCountResponse> ratingCounts
) {
}
