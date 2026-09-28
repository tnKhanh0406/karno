package com.karnofleet.controller.internal;

import com.karnofleet.entity.LinehaulSchedule;
import com.karnofleet.entity.TransportTrip;
import com.karnofleet.entity.Vehicle;
import com.karnofleet.repository.LinehaulScheduleRepo;
import com.karnofleet.repository.TransportTripRepo;
import com.karnofleet.repository.VehicleRepo;
import com.karnofleet.service.TripGeneratorService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/fleet")
@RequiredArgsConstructor
public class FleetInternalController {

    private final VehicleRepo vehicleRepo;
    private final LinehaulScheduleRepo scheduleRepo;
    private final TransportTripRepo tripRepo;
    private final TripGeneratorService tripGeneratorService;

    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleRepo.findAll());
    }

    @GetMapping("/vehicles/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable UUID id) {
        return vehicleRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<LinehaulSchedule>> getAllSchedules() {
        return ResponseEntity.ok(scheduleRepo.findAll());
    }

    @GetMapping("/schedules/active")
    public ResponseEntity<List<LinehaulSchedule>> getActiveSchedules() {
        return ResponseEntity.ok(scheduleRepo.findActiveSchedules());
    }

    @GetMapping("/trips")
    public ResponseEntity<List<TransportTrip>> getTrips(
            @RequestParam(required = false) UUID fromHubId,
            @RequestParam(required = false) UUID toHubId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureTo) {
        return ResponseEntity.ok(tripRepo.findTrips(fromHubId, toHubId, departureFrom, departureTo));
    }

    @GetMapping("/trips/{id}")
    public ResponseEntity<TransportTrip> getTripById(@PathVariable UUID id) {
        return tripRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/trips/generate")
    public ResponseEntity<List<TransportTrip>> generateTrips(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "7") int horizonDays) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.now();
        List<TransportTrip> generated = tripGeneratorService.generateTrips(start, horizonDays);
        return ResponseEntity.ok(generated);
    }
}
