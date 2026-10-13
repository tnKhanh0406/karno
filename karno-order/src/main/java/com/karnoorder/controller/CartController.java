package com.karnoorder.controller;

import com.karnoorder.dto.request.AddToCartRequest;
import com.karnoorder.dto.request.CreateOrderRequest;
import com.karnoorder.dto.request.UUIDListRequest;
import com.karnoorder.dto.request.UpdateCartRequest;
import com.karnoorder.dto.response.AddToCartResponse;
import com.karnoorder.dto.response.CartResponse;
import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.dto.response.UpdateCartResponse;
import com.karnoorder.service.CartService;
import com.karnoorder.service.CheckoutPreparationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
public class CartController {
    private final CartService cartService;
    private final CheckoutPreparationService checkoutSagaService;

    @GetMapping("/{userId}")
    public CartResponse getCart(@PathVariable("userId") UUID userId) {
        return cartService.getCart(userId);
    }

    @PostMapping("/{userId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public AddToCartResponse addToCart(@PathVariable("userId") UUID userId,
                                       @Valid @RequestBody AddToCartRequest request) {
        return cartService.addToCart(userId, request);
    }

    @PutMapping("/{userId}/items/{cartItemId}")
    public UpdateCartResponse updateCartItem(@PathVariable("userId") UUID userId,
                                             @PathVariable("cartItemId") UUID cartItemId,
                                             @Valid @RequestBody UpdateCartRequest request) {
        return cartService.updateCartItem(userId, cartItemId, request);
    }

    @DeleteMapping("/{userId}/items/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromCart(@PathVariable("userId") UUID userId,
                               @PathVariable("cartItemId") UUID cartItemId) {
        cartService.removeFromCart(userId, cartItemId);
    }

    @DeleteMapping("/{userId}/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCartItems(@PathVariable("userId") UUID userId,
                                @RequestBody UUIDListRequest request) {
        cartService.removeCartItems(userId, request);
    }

    @PostMapping("/test-reservation")
    public UUID testReservation(
            @RequestParam UUID customerId,
            @RequestParam UUID variantId,
            @RequestParam Integer quantity,
            @RequestParam String idempotencyKey
    ) {
        return checkoutSagaService.startTestSaga(
                customerId,
                variantId,
                quantity,
                idempotencyKey
        );
    }

    @PostMapping("/prepare-test/{customerId}")
    public ResponseEntity<CheckoutContext> prepare(
            @Valid @RequestBody CreateOrderRequest request,
            @PathVariable UUID customerId
    ) {
        CheckoutContext context =
                checkoutSagaService.prepare(
                        customerId,
                        request
                );

        return ResponseEntity.ok(context);
    }
}
