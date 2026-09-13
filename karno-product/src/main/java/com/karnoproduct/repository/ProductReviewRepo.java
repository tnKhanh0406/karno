package com.karnoproduct.repository;

import com.karnoproduct.dto.response.RatingCountResponse;
import com.karnoproduct.repository.projection.ProductReviewProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ProductReviewRepo {
    List<ProductReviewProjection> getReviewsByProductId(@Param("productId") UUID productId,
                                                        @Param("limit") int limit,
                                                        @Param("offset") int offset,
                                                        @Param("rating") Integer rating);

    long countReviewsByProductId(@Param("productId") UUID productId, @Param("rating") Integer rating);

    List<RatingCountResponse> getRatingCountsByProductId(@Param("productId") UUID productId);
}
