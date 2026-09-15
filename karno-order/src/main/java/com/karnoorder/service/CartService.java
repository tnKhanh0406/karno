package com.karnoorder.service;

import com.karnoorder.dto.response.CartResponse;

import java.util.UUID;

public interface CartService {
    CartResponse getCart(UUID userId);
}
