package com.karnoorder.service;

import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.dto.response.SagaStartResult;

public interface CheckoutSagaStartTxService {
    SagaStartResult persistNewOrGetExisting(CheckoutContext context, String idempotencyKey, String requestHash);
}
