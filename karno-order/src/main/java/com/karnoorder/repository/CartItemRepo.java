package com.karnoorder.repository;

import com.karnoorder.repository.projection.CartItemProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface CartItemRepo {
    int countCartItemsByCartId(@Param("cartId") UUID cartId);
    List<CartItemProjection> findByCartId(@Param("cartId") UUID cartId);
    CartItemProjection findByCartIdAndVariantId(
            @Param("cartId") UUID cartId,
            @Param("variantId") UUID variantId);
    void insertCartItem(
            @Param("id") UUID id,
            @Param("cartId") UUID cartId,
            @Param("productId") UUID productId,
            @Param("variantId") UUID variantId,
            @Param("shopId") UUID shopId,
            @Param("quantity") Integer quantity);

    void updateQuantity(
            @Param("id") UUID id,
            @Param("quantity") Integer quantity);
}
