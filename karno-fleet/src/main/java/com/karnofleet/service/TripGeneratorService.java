package com.karnofleet.service;

import com.karnofleet.entity.TransportTrip;
import java.time.LocalDate;
import java.util.List;

public interface TripGeneratorService {
    List<TransportTrip> generateTrips(LocalDate startDate, int horizonDays);

    List<TransportTrip> generateNextDays(int horizonDays);
}
