package com.karnoproduct.repository;

import com.karnoproduct.dto.response.CategoryResponse;
import com.karnoproduct.dto.response.ImageResponse;
import com.karnoproduct.repository.projection.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ProductDetailRepo {
    ProductDetailProjection getProductDetail(@Param("productId") UUID productId);
    List<VariantProjection> getVariantsByProductId(@Param("productId") UUID productId);
    List<ImageResponse> getImagesByProductId(@Param("productId") UUID productId);
    List<AttributeValueProjection> getAttributesByProductId(@Param("productId") UUID productId);
    List<VariantAttributeValueProjection> getVariantAttributeValuesByProductId(@Param("productId") UUID productId);
    List<CategoryResponse> getCategoryBreadcrumbs(@Param("categoryId") UUID categoryId);
}
