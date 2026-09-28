package com.karnofleet.repository;

import com.karnofleet.entity.TransportTrip;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TransportTripRepo {
    int insert(TransportTrip trip);

    int insertIgnore(TransportTrip trip);

    Optional<TransportTrip> findById(@Param("id") UUID id);

    Optional<TransportTrip> findByTripCode(@Param("tripCode") String tripCode);

    Optional<TransportTrip> findByScheduleIdAndDeparture(
            @Param("scheduleId") UUID scheduleId,
            @Param("scheduledDepartureAt") LocalDateTime scheduledDepartureAt);

    boolean existsByScheduleIdAndDeparture(
            @Param("scheduleId") UUID scheduleId,
            @Param("scheduledDepartureAt") LocalDateTime scheduledDepartureAt);

    List<TransportTrip> findTrips(
            @Param("fromHubId") UUID fromHubId,
            @Param("toHubId") UUID toHubId,
            @Param("departureFrom") LocalDateTime departureFrom,
            @Param("departureTo") LocalDateTime departureTo);

    List<TransportTrip> findByStatus(@Param("status") String status);
}
