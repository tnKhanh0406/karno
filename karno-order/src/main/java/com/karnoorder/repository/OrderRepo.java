package com.karnoorder.repository;

import com.karnoorder.entity.Order;
import com.karnoorder.enums.OrderStatus;
import com.karnoorder.repository.projection.CustomerOrderProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;
import java.util.List;

@Mapper
public interface OrderRepo {
    int insert(@Param("o") Order order);
    int countByCheckoutId(@Param("checkoutId") UUID checkoutId);
    Order findById(@Param("orderId") UUID orderId);
    int markPacked(@Param("orderId") UUID orderId, @Param("shopId") UUID shopId);
    long countCustomerOrders(@Param("customerId") UUID customerId,
                             @Param("status") OrderStatus status,
                             @Param("keyword") String keyword);
    List<CustomerOrderProjection> findCustomerOrders(@Param("customerId") UUID customerId,
                                                     @Param("status") OrderStatus status,
                                                     @Param("keyword") String keyword,
                                                     @Param("limit") int limit,
                                                     @Param("offset") int offset);
    long countShopOrders(@Param("shopId") UUID shopId,
                         @Param("status") OrderStatus status,
                         @Param("keyword") String keyword);
    List<CustomerOrderProjection> findShopOrders(@Param("shopId") UUID shopId,
                                                 @Param("status") OrderStatus status,
                                                 @Param("keyword") String keyword,
                                                 @Param("limit") int limit,
                                                 @Param("offset") int offset);
}
