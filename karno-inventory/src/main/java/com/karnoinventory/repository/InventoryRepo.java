package com.karnoinventory.repository;

import com.karnoinventory.dto.response.VariantInventoryResponse;
import com.karnoinventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mapper
public interface InventoryRepo {
    List<VariantInventoryResponse> getAvailabilityByVariantIds(@Param("variantIds") List<UUID> variantIds);
    List<Inventory> findByVariantIdsForUpdate(@Param("variantIds") Collection<UUID> variantIds);
    int increaseReserved(
            @Param("inventoryId") UUID inventoryId,
            @Param("quantity") int quantity
    );
    int confirmReservedStock(
            @Param("inventoryId") UUID inventoryId,
            @Param("quantity") Integer quantity
    );
    Inventory findByIdForUpdate(
            @Param("inventoryId") UUID inventoryId
    );
}
