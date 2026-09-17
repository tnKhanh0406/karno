package com.karnoproduct.service.impl;

import com.karnoproduct.dto.response.VariantAttributeResponse;
import com.karnoproduct.dto.response.VariantCartResponse;
import com.karnoproduct.dto.response.VariantPurchasableResponse;
import com.karnoproduct.exception.NotFoundException;
import com.karnoproduct.repository.ProductAttributeRepo;
import com.karnoproduct.repository.ProductVariantRepo;
import com.karnoproduct.repository.projection.VariantAttributeProjection;
import com.karnoproduct.repository.projection.VariantCartProjection;
import com.karnoproduct.repository.projection.VariantPurchasableProjection;
import com.karnoproduct.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {
    private final ProductVariantRepo productVariantRepo;
    private final ProductAttributeRepo productAttributeRepo;

    @Override
    public List<VariantCartResponse> getVariantCartResponses(List<UUID> variantIds) {
        List<VariantCartProjection> variantCarts = productVariantRepo.getCartInfoByVariantIds(variantIds);
        List<VariantAttributeProjection> variantAttributes = productAttributeRepo.getAttributesByVariantIds(variantIds);

        Map<UUID, List<VariantAttributeResponse>> variantAttributesMap = new HashMap<>();
        for (VariantAttributeProjection attribute : variantAttributes) {
            variantAttributesMap.computeIfAbsent(attribute.getVariantId(), k -> new ArrayList<>())
                    .add(new VariantAttributeResponse(
                            attribute.getAttributeId(),
                            attribute.getAttributeName(),
                            attribute.getAttributeValueId(),
                            attribute.getAttributeValueName()
                    ));
        }

        return variantCarts.stream()
                .map(variant -> new VariantCartResponse(
                        variant.getId(),
                        variant.getName(),
                        variant.getPrimaryImg(),
                        variant.getPrice(),
                        variant.getProductId(),
                        variant.getProductName(),
                        variant.getThumbnailImg(),
                        variant.getShopId(),
                        variant.getProductStatus().toString(),
                        variantAttributesMap.getOrDefault(variant.getId(), Collections.emptyList())
                ))
                .toList();
    }

    @Override
    public VariantPurchasableResponse getVariantPurchasableInfo(UUID variantId) {
        VariantPurchasableProjection variant = productVariantRepo.getPurchasableInfo(variantId);
        if (variant == null) {
            throw new NotFoundException("Variant not found");
        }
        boolean sellable = "ACTIVE".equalsIgnoreCase(variant.getProductStatus());
        return new VariantPurchasableResponse(
                variant.getVariantId(),
                variant.getPrimaryImg(),
                variant.getPrice(),
                variant.getProductId(),
                variant.getProductName(),
                variant.getThumbnailImg(),
                variant.getShopId(),
                sellable
        );
    }
}
