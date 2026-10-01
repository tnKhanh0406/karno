package com.karnofleet.service.impl;

import com.karnofleet.dto.response.FirstMileContext;
import com.karnofleet.dto.response.HubInfo;
import com.karnofleet.entity.ShipperShift;
import com.karnofleet.entity.Vehicle;
import com.karnofleet.repository.ShipperShiftRepo;
import com.karnofleet.repository.VehicleRepo;
import com.karnofleet.service.FirstMileContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FirstMileContextServiceImpl implements FirstMileContextService {

    private final ShipperShiftRepo shipperShiftRepo;
    private final VehicleRepo vehicleRepo;

    @Override
    public FirstMileContext getContext(UUID shipperUserId, LocalDateTime now) {
        ShipperShift shift = shipperShiftRepo.findActiveShift(shipperUserId, now);
        if (shift == null) {
            throw new IllegalStateException("No active shift found");
        }

        Vehicle vehicle = vehicleRepo.findById(shift.getVehicleId())
                .orElseThrow(() -> new IllegalStateException("Vehicle not found for shift"));

        HubInfo hub = new HubInfo(
                shift.getHubId(),
                "HN01",
                "Hà Nội Hub",
                new BigDecimal("21.028500"),
                new BigDecimal("105.854200"),
                "ACTIVE"
        );
        return new FirstMileContext(
                shipperUserId,
                shift.getId(),
                shift.getStartsAt(),
                shift.getEndsAt(),
                vehicle,
                hub
        );
    }
}
