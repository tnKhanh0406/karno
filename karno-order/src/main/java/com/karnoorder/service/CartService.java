package com.karnoorder.service;

import com.karnoorder.dto.request.AddToCartRequest;
import com.karnoorder.dto.request.UUIDListRequest;
import com.karnoorder.dto.request.UpdateCartRequest;
import com.karnoorder.dto.response.AddToCartResponse;
import com.karnoorder.dto.response.CartResponse;
import com.karnoorder.dto.response.UpdateCartResponse;

import java.util.UUID;

public interface CartService {
    CartResponse getCart(UUID userId);
    AddToCartResponse addToCart(UUID userId, AddToCartRequest request);
    UpdateCartResponse updateCartItem(UUID userId, UUID cartItemId, UpdateCartRequest request);
    void removeFromCart(UUID userId, UUID cartItemId);
    void removeCartItems(UUID userId, UUIDListRequest request);
}
