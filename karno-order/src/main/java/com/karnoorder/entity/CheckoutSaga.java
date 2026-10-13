package com.karnoorder.entity;

import com.karnoorder.enums.CheckoutSagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSaga {
    private UUID id;
    private UUID customerId;
    private String idempotencyKey;
    private String requestHash;
    private CheckoutSagaStatus status;
    private String contextPayload;
    private String resultPayload;
    private String failureCode;
    private String failureMessage;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
}
