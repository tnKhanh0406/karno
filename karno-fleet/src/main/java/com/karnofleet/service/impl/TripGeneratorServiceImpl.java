package com.karnofleet.service.impl;

import com.karnofleet.entity.LinehaulSchedule;
import com.karnofleet.entity.TransportTrip;
import com.karnofleet.enums.TripStatus;
import com.karnofleet.repository.LinehaulScheduleRepo;
import com.karnofleet.repository.TransportTripRepo;
import com.karnofleet.service.TripGeneratorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripGeneratorServiceImpl implements TripGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final LinehaulScheduleRepo scheduleRepo;
    private final TransportTripRepo tripRepo;

    @Override
    @Transactional
    public List<TransportTrip> generateTrips(LocalDate startDate, int horizonDays) {
        log.info("Starting trip generation from date {} for horizon {} days", startDate, horizonDays);
        List<LinehaulSchedule> activeSchedules = scheduleRepo.findActiveSchedules();
        List<TransportTrip> generatedTrips = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < horizonDays; i++) {
            LocalDate currentDate = startDate.plusDays(i);

            for (LinehaulSchedule schedule : activeSchedules) {
                LocalDateTime scheduledDepartureAt = LocalDateTime.of(
                        currentDate,
                        schedule.getDepartureTime()
                );
                LocalDateTime plannedArrivalAt = scheduledDepartureAt.plusMinutes(
                        schedule.getPlannedDurationMinutes()
                );

                boolean exists = tripRepo.existsByScheduleIdAndDeparture(
                        schedule.getId(),
                        scheduledDepartureAt
                );

                if (!exists) {
                    String tripCode = String.format("TRIP-%s-%s",
                            schedule.getScheduleCode(),
                            DATE_FORMATTER.format(currentDate)
                    );

                    TransportTrip trip = TransportTrip.builder()
                            .id(UUID.randomUUID())
                            .tripCode(tripCode)
                            .scheduleId(schedule.getId())
                            .fromHubId(schedule.getFromHubId())
                            .toHubId(schedule.getToHubId())
                            .vehicleId(schedule.getVehicleId())
                            .scheduledDepartureAt(scheduledDepartureAt)
                            .plannedArrivalAt(plannedArrivalAt)
                            .estimatedArrivalAt(plannedArrivalAt)
                            .capacityWeight(schedule.getCapacityWeight() != null
                                    ? schedule.getCapacityWeight()
                                    : BigDecimal.ZERO)
                            .reservedWeight(BigDecimal.ZERO)
                            .status(TripStatus.PLANNED)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    int inserted = tripRepo.insertIgnore(trip);
                    if (inserted > 0) {
                        generatedTrips.add(trip);
                    }
                }
            }
        }

        log.info("Finished trip generation. Total new trips created: {}", generatedTrips.size());
        return generatedTrips;
    }

    @Override
    public List<TransportTrip> generateNextDays(int horizonDays) {
        return generateTrips(LocalDate.now(), horizonDays);
    }
}
