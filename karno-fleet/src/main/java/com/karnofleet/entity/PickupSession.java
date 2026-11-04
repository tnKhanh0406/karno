package com.karnofleet.entity;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PickupSession {
    private UUID id;
    private UUID shipperId;
    private UUID hubId;
    private UUID zoneId;
    private UUID vehicleId;
    private BigDecimal startLatitude;
    private BigDecimal startLongitude;
    private LocalDateTime plannedStartAt;
    private LocalDateTime desiredReturnAt;
    private String status;
    private String routePlan;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
