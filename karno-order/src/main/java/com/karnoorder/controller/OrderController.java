package com.karnoorder.controller;

import com.karnoorder.dto.request.CreateOrderRequest;
import com.karnoorder.dto.response.CheckoutStartResponse;
import com.karnoorder.dto.response.CheckoutStatusResponse;
import com.karnoorder.service.OrderApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    @PostMapping("/{customerId}")
    public ResponseEntity<CheckoutStartResponse> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request,
            @PathVariable("customerId") UUID customerId
    ) {
        CheckoutStartResponse response = orderApplicationService.createOrder(customerId, idempotencyKey, request);

        HttpStatus httpStatus =
                switch (response.status()) {
                    case COMPLETED, FAILED -> HttpStatus.OK;
                    default -> HttpStatus.ACCEPTED;
                };

        return ResponseEntity
                .status(httpStatus)
                .body(response);
    }

    @GetMapping("/checkouts/{checkoutId}/{customerId}")
    public CheckoutStatusResponse getCheckoutStatus(
            @PathVariable UUID checkoutId,
            @PathVariable UUID customerId
    ) {
        return orderApplicationService.getCheckoutStatus(customerId, checkoutId);
    }
}
