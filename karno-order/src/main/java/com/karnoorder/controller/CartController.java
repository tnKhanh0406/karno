package com.karnoorder.controller;

import com.karnoorder.dto.request.AddToCartRequest;
import com.karnoorder.dto.response.AddToCartResponse;
import com.karnoorder.dto.response.CartResponse;
import com.karnoorder.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
public class CartController {
    private final CartService cartService;

    @GetMapping("/{userId}")
    public CartResponse getCart(@PathVariable("userId") UUID userId) {
        return cartService.getCart(userId);
    }

    @PostMapping("/{userId}/items")
    public AddToCartResponse addToCart(@PathVariable("userId") UUID userId,
                                       @RequestBody AddToCartRequest request) {
        return cartService.addToCart(userId, request);
    }
}
