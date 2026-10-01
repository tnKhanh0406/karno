package com.karnofleet.dto.response;

import com.karnofleet.entity.Vehicle;

import java.time.LocalDateTime;
import java.util.UUID;

public record FirstMileContext(
        UUID shipperUserId,
        UUID shiftId,
        LocalDateTime shiftStartsAt,
        LocalDateTime shiftEndsAt,
        Vehicle vehicle,
        HubInfo hub
) {
}
