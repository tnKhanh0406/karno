package com.karnoshipping.repository;

import com.karnoshipping.entity.Shipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface ShipmentRepo {
    int insert(Shipment shipment);
    Shipment findByOrderId(@Param("orderId") UUID orderId);
}
