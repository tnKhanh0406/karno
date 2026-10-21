package com.karnoshipping.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface ShipmentStatusHistoryRepo {
    int insertInitial(@Param("id") UUID id, @Param("shipmentId") UUID shipmentId,
                      @Param("status") String status);
}
