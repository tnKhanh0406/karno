package com.karnoorder.service;

import com.karnoorder.dto.request.AddToCartRequest;
import com.karnoorder.dto.response.AddToCartResponse;
import com.karnoorder.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {
    CartResponse getCart(UUID userId);
    AddToCartResponse addToCart(UUID userId, AddToCartRequest request);
}
