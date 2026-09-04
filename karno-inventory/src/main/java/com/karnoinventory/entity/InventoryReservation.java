package com.karnoinventory.entity;

import com.karnoinventory.enums.ReservationStatus;
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
public class InventoryReservation {
    private UUID id;
    private UUID inventoryId;
    private UUID orderId;
    private UUID orderItemId;
    private Integer quantity;
    private ReservationStatus status;
    private OffsetDateTime expiresAt;
    private OffsetDateTime confirmedAt;
    private OffsetDateTime releasedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}