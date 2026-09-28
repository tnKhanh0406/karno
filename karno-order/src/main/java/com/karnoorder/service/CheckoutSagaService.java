package com.karnoorder.service;

import java.util.UUID;

public interface CheckoutSagaService {
    UUID startTestSaga(UUID customerId, UUID variantId, Integer quantity, String idempotencyKey);
}
