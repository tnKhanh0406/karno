package com.karnofleet.entity;

import com.karnofleet.enums.ShipperShiftStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipperShift {
    private UUID id;
    private UUID shipperUserId;
    private UUID vehicleId;
    private UUID hubId;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private ShipperShiftStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
