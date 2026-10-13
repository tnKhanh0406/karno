package com.karnoorder.dto.response;

import com.karnoorder.entity.CheckoutSaga;

public record SagaStartResult(
        CheckoutSaga saga,
        boolean created
) {}
