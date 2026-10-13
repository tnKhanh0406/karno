package com.karnoproduct.repository;

import com.karnoproduct.dto.response.VariantCheckoutInfo;
import com.karnoproduct.repository.projection.VariantCartProjection;
import com.karnoproduct.repository.projection.VariantPurchasableProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ProductVariantRepo {
    List<VariantCartProjection> getCartInfoByVariantIds(@Param("variantIds") List<UUID> variantIds);
    VariantPurchasableProjection getPurchasableInfo(@Param("variantId") UUID variantId);
    List<VariantCheckoutInfo> getCheckoutInfoByVariantIds(@Param("variantIds") List<UUID> variantIds);
}
