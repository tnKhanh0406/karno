package com.karnoproduct.repository;

import com.karnoproduct.repository.projection.VariantAttributeProjection;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.UUID;

@Mapper
public interface ProductAttributeRepo {
    List<VariantAttributeProjection> getAttributesByVariantIds(List<UUID> variantIds);
}
