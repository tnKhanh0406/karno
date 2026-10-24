package com.karnouser.entity;

import com.karnouser.enums.ShopStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shop {
    private UUID id;
    private UUID ownerId;
    private UUID originHubId;
    private UUID pickupZoneId;
    private String name;
    private String description;
    private String logoUrl;
    private ShopStatus status;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
