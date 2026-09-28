package com.karnofleet.entity;

import com.karnofleet.enums.TripStatus;
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
public class TransportTrip {
    private UUID id;
    private String tripCode;
    private UUID scheduleId;
    private UUID fromHubId;
    private UUID toHubId;
    private UUID vehicleId;
    private LocalDateTime scheduledDepartureAt;
    private LocalDateTime plannedArrivalAt;
    private LocalDateTime estimatedArrivalAt;
    private LocalDateTime actualDepartureAt;
    private LocalDateTime actualArrivalAt;
    private BigDecimal capacityWeight;
    private BigDecimal reservedWeight;
    private TripStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
