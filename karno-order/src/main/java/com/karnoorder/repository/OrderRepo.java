package com.karnoorder.repository;

import com.karnoorder.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface OrderRepo {
    int insert(@Param("o") Order order);
    int countByCheckoutId(@Param("checkoutId") UUID checkoutId);
    Order findById(@Param("orderId") UUID orderId);
    int markPacked(@Param("orderId") UUID orderId, @Param("shopId") UUID shopId);
}
