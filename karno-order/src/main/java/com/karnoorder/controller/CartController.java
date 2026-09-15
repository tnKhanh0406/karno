package com.karnoorder.controller;

import com.karnoorder.dto.response.CartResponse;
import com.karnoorder.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
