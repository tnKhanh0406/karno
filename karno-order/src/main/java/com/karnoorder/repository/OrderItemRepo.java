package com.karnoorder.repository;

import com.karnoorder.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderItemRepo {
    int insert(@Param("i") OrderItem item);
}
