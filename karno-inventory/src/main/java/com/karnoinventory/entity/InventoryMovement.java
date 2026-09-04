package com.karnoinventory.entity;

import com.karnoinventory.enums.InventoryActorType;
import com.karnoinventory.enums.InventoryMovementType;
import com.karnoinventory.enums.InventoryReferenceType;
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
public class InventoryMovement {
    private UUID id;
    private UUID inventoryId;
    private InventoryMovementType type;
    private Integer quantity;
    private Integer onHandBefore;
    private Integer onHandAfter;
    private Integer reservedBefore;
    private Integer reservedAfter;
    private InventoryReferenceType referenceType;
    private UUID referenceId;
    private String reason;
    private InventoryActorType actorType;
    private UUID actorId;
    private OffsetDateTime createdAt;
}