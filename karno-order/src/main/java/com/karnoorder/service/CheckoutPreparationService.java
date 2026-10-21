package com.karnoorder.service;

import com.karnoorder.dto.request.CreateOrderRequest;
import com.karnoorder.dto.request.CheckoutPreviewRequest;
import com.karnoorder.dto.response.CheckoutContext;

import java.util.UUID;

public interface CheckoutPreparationService {
    UUID startTestSaga(UUID customerId, UUID variantId, Integer quantity, String idempotencyKey);
    CheckoutContext prepare(UUID customerId, CreateOrderRequest request);
    CheckoutContext preview(UUID customerId, CheckoutPreviewRequest request);
}
