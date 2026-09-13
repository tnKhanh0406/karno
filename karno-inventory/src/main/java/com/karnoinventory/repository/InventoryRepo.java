package com.karnoinventory.repository;

import com.karnoinventory.dto.response.VariantInventoryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface InventoryRepo {
    List<VariantInventoryResponse> getAvailabilityByVariantIds(@Param("variantIds") List<UUID> variantIds);
}
