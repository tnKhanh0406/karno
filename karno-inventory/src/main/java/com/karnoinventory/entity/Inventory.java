package com.karnoinventory.entity;

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
public class Inventory {
    private UUID id;
    private UUID shopId;
    private UUID variantId;
    private Integer onHandQuantity;
    private Integer reservedQuantity;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
