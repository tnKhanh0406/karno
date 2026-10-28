package com.karnoorder.repository;

import com.karnoorder.entity.OrderItem;
import com.karnoorder.dto.response.CustomerOrderItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface OrderItemRepo {
    int insert(@Param("i") OrderItem item);
    List<CustomerOrderItemResponse> findByOrderIds(@Param("orderIds") List<UUID> orderIds);
}
