package com.karnoorder.entity;

import com.karnoorder.enums.ActorType;
import com.karnoorder.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {
    private UUID id;
    private UUID orderId;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private ActorType actorType;
    private UUID actorId;
    private String description;
    private OffsetDateTime occurredAt;
}