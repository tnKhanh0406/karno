package com.karnofleet.entity;

import com.karnofleet.enums.VehicleStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Vehicle {
    private UUID id;
    private String vehicleCode;
    private String licensePlate;
    private BigDecimal capacityWeight;
    private BigDecimal capacityVolume;
    private VehicleStatus status;
    private String ownershipType;
    private UUID ownerShipperId;
    private UUID hubId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
