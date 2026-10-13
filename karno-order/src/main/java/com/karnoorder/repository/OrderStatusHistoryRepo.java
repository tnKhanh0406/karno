package com.karnoorder.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface OrderStatusHistoryRepo {

    int insertInitial(
            @Param("id") UUID id,
            @Param("orderId") UUID orderId,
            @Param("status") String status
    );
}