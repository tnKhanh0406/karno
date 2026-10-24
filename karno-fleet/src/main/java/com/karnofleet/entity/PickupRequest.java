package com.karnofleet.entity;

import com.karnofleet.enums.PickupRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupRequest {
    private UUID id;
    private String requestCode;
    private UUID shopId;
    private UUID sellerId;
    private String shopName;
    private String pickupAddress;
    private BigDecimal pickupLatitude;
    private BigDecimal pickupLongitude;
    private UUID targetHubId;
    private UUID pickupZoneId;
    private UUID pickupSessionId;
    private UUID assignedShipperId;
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;
    private Integer serviceDurationSeconds;
    private LocalTime pickupWindowStart;
    private LocalTime pickupWindowEnd;
    private PickupRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime pickedUpAt;
}
