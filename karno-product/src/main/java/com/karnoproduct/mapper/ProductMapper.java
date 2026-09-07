package com.karnoproduct.mapper;

import com.karnoproduct.dto.response.ProductListItemResponse;
import com.karnoproduct.repository.projection.ProductListProjection;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductListItemResponse toListItemResponse(ProductListProjection product) {
        return new ProductListItemResponse(
                product.getId(),
                product.getName(),
                product.getThumbnailImg(),
                product.getMinPrice(),
                product.getRating(),
                product.getSoldCount()
        );
    }
}