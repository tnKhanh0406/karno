package com.karnofleet.entity;

import com.karnofleet.enums.VehicleStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    private UUID id;
    private String vehicleCode;
    private String licensePlate;
    private BigDecimal capacityWeight;
    private BigDecimal capacityVolume;
    private VehicleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
