package com.karnoorder.repository;

import com.karnoorder.repository.projection.CartItemProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface CartItemRepo {
    int countCartItemsByCartId(@Param("cartId") UUID cartId);
    List<CartItemProjection> getCartItemsByCartId(@Param("cartId") UUID cartId);
}
