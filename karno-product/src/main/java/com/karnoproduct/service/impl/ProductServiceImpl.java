package com.karnoproduct.service.impl;

import com.karnoproduct.client.KarnoInventoryClient;
import com.karnoproduct.client.KarnoUserClient;
import com.karnoproduct.dto.request.InventoryAvailabilityRequest;
import com.karnoproduct.dto.request.ProductSearchRequest;
import com.karnoproduct.dto.response.*;
import com.karnoproduct.mapper.ProductMapper;
import com.karnoproduct.repository.ProductDetailRepo;
import com.karnoproduct.repository.ProductRepo;
import com.karnoproduct.repository.ProductReviewRepo;
import com.karnoproduct.repository.projection.*;
import com.karnoproduct.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepo productRepo;
    private final ProductMapper productMapper;
    private final KarnoUserClient karnoUserClient;
    private final ProductDetailRepo productDetailRepo;
    private final KarnoInventoryClient karnoInventoryClient;
    private final ProductReviewRepo productReviewRepo;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 100;

    @Override
    public PageResponse<ProductListItemResponse> getProductList(ProductSearchRequest request) {
        int page = request.page() != null? Math.max(request.page(), 0) : DEFAULT_PAGE;
        int size = request.size() != null ? Math.min(Math.max(request.size(), 1), MAX_SIZE) : DEFAULT_SIZE;
        int offset = page * size;

        List<ProductListProjection> products = productRepo.searchProducts(request, offset, size);
        Long totalElements = productRepo.countProducts(request);

        List<ProductListItemResponse> productResponses = products.stream()
                .map(productMapper::toListItemResponse)
                .toList();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(productResponses, page, size, totalElements, totalPages);
    }

    @Override
    public ProductDetailResponse getProductDetail(UUID productId) {
        ProductDetailProjection productDetail = productDetailRepo.getProductDetail(productId);
        List<VariantProjection> variants = productDetailRepo.getVariantsByProductId(productId);
        List<ImageResponse> images = productDetailRepo.getImagesByProductId(productId);
        List<CategoryResponse> breadcrumbs = productDetailRepo.getCategoryBreadcrumbs(productDetail.getCategoryId());
        List<AttributeValueProjection> attributes = productDetailRepo.getAttributesByProductId(productId);
        List<VariantAttributeValueProjection> variantAttributeValues = productDetailRepo.getVariantAttributeValuesByProductId(productId);
        ShopInformationResponse shopInformation = karnoUserClient.getShopById(productDetail.getShopId());
        long reviewCount = productReviewRepo.countReviewsByProductId(productId, null);
        List<RatingCountResponse> ratingCounts = productReviewRepo.getRatingCountsByProductId(productId);

        RatingSummaryResponse ratingSummary = new RatingSummaryResponse(reviewCount, ratingCounts);

        List<UUID> variantIds = variants.stream()
                .map(VariantProjection::getId)
                .toList();

        List<VariantInventoryResponse> inventories = karnoInventoryClient.getAvailabilityByVariantIds(new InventoryAvailabilityRequest(variantIds));

        List<ProductAttributeResponse> attributeResponses = buildAttributeResponses(attributes);

        List<VariantResponse> variantResponses = buildVariantResponses(variants, variantAttributeValues, inventories);

        return new ProductDetailResponse(
                productDetail.getId(),
                productDetail.getName(),
                productDetail.getDescription(),
                productDetail.getThumbnailImg(),
                productDetail.getSoldCount(),
                productDetail.getRating(),
                productDetail.getMinPrice(),
                productDetail.getMaxPrice(),
                breadcrumbs,
                images,
                variantResponses,
                attributeResponses,
                shopInformation,
                ratingSummary
        );
    }

    private List<ProductAttributeResponse> buildAttributeResponses(List<AttributeValueProjection> attributes) {
        Map<UUID, List<AttributeValueProjection>> groupedAttributes = new LinkedHashMap<>();
        for (AttributeValueProjection attribute : attributes) {
            groupedAttributes.computeIfAbsent(attribute.getAttributeId(), k -> new ArrayList<>())
                    .add(attribute);
        }
        List<ProductAttributeResponse> attributeResponses = new ArrayList<>();
        for (List<AttributeValueProjection> attributeGroup : groupedAttributes.values()) {
            if (attributeGroup.isEmpty()) {
                continue;
            }
            AttributeValueProjection first = attributeGroup.getFirst();
            List<AttributeValueResponse> attributeValueResponses = attributeGroup.stream()
                    .map(attr -> new AttributeValueResponse(attr.getValueId(), attr.getValue()))
                    .toList();

            ProductAttributeResponse attributeResponse = new ProductAttributeResponse(
                    first.getAttributeId(),
                    first.getAttributeName(),
                    attributeValueResponses
            );
            attributeResponses.add(attributeResponse);
        }
        return attributeResponses;
    }

    private List<VariantResponse> buildVariantResponses(List<VariantProjection> variants,
                                                        List<VariantAttributeValueProjection> variantAttributeValues,
                                                        List<VariantInventoryResponse> inventories) {
        Map<UUID, List<UUID>> attributeValueIdsByVariantId = new HashMap<>();
        for (VariantAttributeValueProjection variantAttributeValue : variantAttributeValues) {
            attributeValueIdsByVariantId.computeIfAbsent(variantAttributeValue.getVariantId(), k -> new ArrayList<>())
                    .add(variantAttributeValue.getAttributeValueId());
        }

        Map<UUID, Integer> availableQuantityByVariantId = new HashMap<>();
        for (VariantInventoryResponse inventory : inventories) {
            availableQuantityByVariantId.put(
                    inventory.variantId(),
                    inventory.availableQuantity()
            );
        }
        return variants.stream()
                .map(variant -> {
                    List<UUID> attributeValueIds = attributeValueIdsByVariantId.getOrDefault(variant.getId(), Collections.emptyList());
                    Integer availableQuantity = availableQuantityByVariantId.getOrDefault(variant.getId(), 0);
                    return new VariantResponse(
                            variant.getId(),
                            variant.getSku(),
                            variant.getName(),
                            variant.getPrice(),
                            variant.getPrimaryImg(),
                            availableQuantity,
                            attributeValueIds
                    );
                })
                .toList();
    }
}
