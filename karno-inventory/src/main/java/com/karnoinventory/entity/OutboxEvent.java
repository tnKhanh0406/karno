package com.karnoinventory.entity;

import com.karnoinventory.enums.OutboxEventStatus;
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
public class OutboxEvent {
    private UUID id;
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    private String topic;
    private String payload;
    private OutboxEventStatus status;
    private Integer retryCount;
    private Instant createdAt;
    private Instant publishedAt;
    private Instant updatedAt;
}