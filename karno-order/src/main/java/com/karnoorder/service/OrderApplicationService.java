package com.karnoorder.service;

import com.karnoorder.dto.request.CreateOrderRequest;
import com.karnoorder.dto.response.CheckoutStartResponse;
import com.karnoorder.dto.response.CheckoutStatusResponse;
import com.karnoorder.dto.request.PackOrderRequest;
import com.karnoorder.dto.response.PackOrderResponse;

import java.util.UUID;

public interface OrderApplicationService {
    CheckoutStartResponse createOrder(UUID customerId, String idempotencyKey, CreateOrderRequest request);
    CheckoutStatusResponse getCheckoutStatus(UUID customerId, UUID checkoutId);
    PackOrderResponse packOrder(UUID orderId, PackOrderRequest request);
}
