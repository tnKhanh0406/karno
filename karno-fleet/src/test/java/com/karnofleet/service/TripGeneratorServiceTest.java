package com.karnofleet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.karnofleet.entity.LinehaulSchedule;
import com.karnofleet.entity.TransportTrip;
import com.karnofleet.enums.ScheduleFrequency;
import com.karnofleet.enums.ScheduleStatus;
import com.karnofleet.enums.TripStatus;
import com.karnofleet.repository.LinehaulScheduleRepo;
import com.karnofleet.repository.TransportTripRepo;
import com.karnofleet.service.impl.TripGeneratorServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripGeneratorServiceTest {

    @Mock
    private LinehaulScheduleRepo scheduleRepo;

    @Mock
    private TransportTripRepo tripRepo;

    @InjectMocks
    private TripGeneratorServiceImpl tripGeneratorService;

    private LinehaulSchedule scheduleHnHcm;

    @BeforeEach
    void setUp() {
        scheduleHnHcm = LinehaulSchedule.builder()
                .id(UUID.fromString("d2000000-0000-0000-0000-000000000001"))
                .scheduleCode("S-HN-HCM-2200")
                .fromHubId(UUID.fromString("c1000000-0000-0000-0000-000000000001"))
                .toHubId(UUID.fromString("c1000000-0000-0000-0000-000000000002"))
                .departureTime(LocalTime.of(22, 0))
                .frequency(ScheduleFrequency.DAILY)
                .plannedDurationMinutes(480)
                .capacityWeight(BigDecimal.valueOf(5000))
                .capacityVolume(BigDecimal.valueOf(25))
                .vehicleId(UUID.fromString("d1000000-0000-0000-0000-000000000001"))
                .status(ScheduleStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGenerateTrips_GeneratesSevenDays() {
        LocalDate startDate = LocalDate.of(2026, 9, 21);
        when(scheduleRepo.findActiveSchedules()).thenReturn(List.of(scheduleHnHcm));
        when(tripRepo.existsByScheduleIdAndDeparture(any(UUID.class), any(LocalDateTime.class))).thenReturn(false);
        when(tripRepo.insertIgnore(any(TransportTrip.class))).thenReturn(1);

        List<TransportTrip> trips = tripGeneratorService.generateTrips(startDate, 7);

        assertEquals(7, trips.size());
        verify(tripRepo, times(7)).insertIgnore(any(TransportTrip.class));

        TransportTrip firstTrip = trips.get(0);
        assertEquals("TRIP-S-HN-HCM-2200-20260921", firstTrip.getTripCode());
        assertEquals(LocalDateTime.of(2026, 9, 21, 22, 0), firstTrip.getScheduledDepartureAt());
        assertEquals(LocalDateTime.of(2026, 9, 22, 6, 0), firstTrip.getPlannedArrivalAt());
        assertEquals(TripStatus.PLANNED, firstTrip.getStatus());
        assertEquals(BigDecimal.valueOf(5000), firstTrip.getCapacityWeight());
    }

    @Test
    void testGenerateTrips_Idempotency_SkipsExistingTrips() {
        LocalDate startDate = LocalDate.of(2026, 9, 21);
        when(scheduleRepo.findActiveSchedules()).thenReturn(List.of(scheduleHnHcm));

        // Simulate all trips already exist in database
        when(tripRepo.existsByScheduleIdAndDeparture(any(UUID.class), any(LocalDateTime.class))).thenReturn(true);

        List<TransportTrip> trips = tripGeneratorService.generateTrips(startDate, 7);

        assertTrue(trips.isEmpty());
        verify(tripRepo, never()).insertIgnore(any(TransportTrip.class));
    }

    @Test
    void testGenerateTrips_MidnightCrossingArrivalCalculation() {
        LocalDate startDate = LocalDate.of(2026, 9, 21);
        when(scheduleRepo.findActiveSchedules()).thenReturn(List.of(scheduleHnHcm));
        when(tripRepo.existsByScheduleIdAndDeparture(any(UUID.class), any(LocalDateTime.class))).thenReturn(false);
        when(tripRepo.insertIgnore(any(TransportTrip.class))).thenReturn(1);

        List<TransportTrip> trips = tripGeneratorService.generateTrips(startDate, 1);

        assertEquals(1, trips.size());
        TransportTrip trip = trips.get(0);

        // Departure 22:00 + 480 mins (8 hours) = next day 06:00
        assertEquals(LocalDateTime.of(2026, 9, 21, 22, 0), trip.getScheduledDepartureAt());
        assertEquals(LocalDateTime.of(2026, 9, 22, 6, 0), trip.getPlannedArrivalAt());
        assertEquals(trip.getPlannedArrivalAt(), trip.getEstimatedArrivalAt());
    }

    @Test
    void testGenerateTrips_EmptySchedules() {
        LocalDate startDate = LocalDate.of(2026, 9, 21);
        when(scheduleRepo.findActiveSchedules()).thenReturn(Collections.emptyList());

        List<TransportTrip> trips = tripGeneratorService.generateTrips(startDate, 7);

        assertNotNull(trips);
        assertTrue(trips.isEmpty());
        verify(tripRepo, never()).insertIgnore(any(TransportTrip.class));
    }
}
