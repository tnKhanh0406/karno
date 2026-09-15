package com.karnoproduct.service.impl;

import com.karnoproduct.client.KarnoUserClient;
import com.karnoproduct.dto.request.UUIDListRequest;
import com.karnoproduct.dto.response.PageResponse;
import com.karnoproduct.dto.response.ProductReviewResponse;
import com.karnoproduct.dto.response.ReviewUserResponse;
import com.karnoproduct.exception.IllegalRequestException;
import com.karnoproduct.repository.ProductReviewRepo;
import com.karnoproduct.repository.projection.ProductReviewProjection;
import com.karnoproduct.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {
    private final ProductReviewRepo productReviewRepo;
    private final KarnoUserClient karnoUserClient;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 8;
    private static final int MAX_SIZE = 12;

    @Override
    public PageResponse<ProductReviewResponse> getReviewsByProductId(UUID productId, Integer page, Integer size, Integer rating) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalRequestException("rating should be between 1 and 5");
        }
        page = page != null ? Math.max(page, 0) : DEFAULT_PAGE;
        size = size != null ? Math.min(Math.max(size, 1), MAX_SIZE) : DEFAULT_SIZE;
        int offset = page * size;
        List<ProductReviewProjection> reviews = productReviewRepo.getReviewsByProductId(productId, size, offset, rating);
        long totalElements = productReviewRepo.countReviewsByProductId(productId, rating);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<UUID> userIds = reviews.stream()
                .map(ProductReviewProjection::getUserId)
                .distinct()
                .toList();

        List<ReviewUserResponse> users =
                userIds.isEmpty() ? Collections.emptyList() : karnoUserClient.getUsersByIds(new UUIDListRequest(userIds));

        Map<UUID, ReviewUserResponse> userById = new HashMap<>();

        for (ReviewUserResponse user : users) {
            userById.put(user.id(), user);
        }

        List<ProductReviewResponse> content = reviews.stream()
                .map(review -> new ProductReviewResponse(
                        review.getId(),
                        review.getComment(),
                        review.getRating(),
                        review.getCreatedAt(),
                        review.getUpdatedAt(),
                        userById.get(review.getUserId())
                ))
                .toList();
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
