package com.karnoproduct.repository;

import com.karnoproduct.repository.projection.VariantCartProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ProductVariantRepo {
    List<VariantCartProjection> getCartInfoByVariantIds(@Param("variantIds") List<UUID> variantIds);
}
