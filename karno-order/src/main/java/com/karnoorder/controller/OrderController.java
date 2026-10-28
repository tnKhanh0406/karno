package com.karnoorder.controller;

import com.karnoorder.dto.request.CreateOrderRequest;
import com.karnoorder.dto.request.CustomerOrderSearchRequest;
import com.karnoorder.dto.request.PackOrderRequest;
import com.karnoorder.dto.response.CheckoutStartResponse;
import com.karnoorder.dto.response.CheckoutStatusResponse;
import com.karnoorder.dto.response.PackOrderResponse;
import com.karnoorder.dto.response.CustomerOrderResponse;
import com.karnoorder.dto.response.PageResponse;
import com.karnoorder.service.OrderApplicationService;
import com.karnoorder.service.OrderQueryService;
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
    private final OrderQueryService orderQueryService;

    @GetMapping("/customers/{customerId}")
    public PageResponse<CustomerOrderResponse> getCustomerOrders(
            @PathVariable UUID customerId,
            @ModelAttribute CustomerOrderSearchRequest request
    ) {
        return orderQueryService.getCustomerOrders(customerId, request);
    }

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

    @PatchMapping("/{orderId}/pack")
    public PackOrderResponse packOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody PackOrderRequest request
    ) {
        return orderApplicationService.packOrder(orderId, request);
    }
}
