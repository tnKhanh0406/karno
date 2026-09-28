package com.karnofleet.entity;

import com.karnofleet.enums.ScheduleFrequency;
import com.karnofleet.enums.ScheduleStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinehaulSchedule {
    private UUID id;
    private String scheduleCode;
    private UUID fromHubId;
    private UUID toHubId;
    private LocalTime departureTime;
    private ScheduleFrequency frequency;
    private Integer plannedDurationMinutes;
    private BigDecimal capacityWeight;
    private BigDecimal capacityVolume;
    private UUID vehicleId;
    private ScheduleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
