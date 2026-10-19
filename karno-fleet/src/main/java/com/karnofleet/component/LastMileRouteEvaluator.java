package com.karnofleet.component;

import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.LastMileOrder.DeliveryMethod;
import com.karnofleet.dto.request.LastMileOrder.DeliveryWindow;
import com.karnofleet.dto.response.LastMileLoadResponse.SelectedCluster;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LastMileRouteEvaluator {
    private static final LocalTime ADMIN_START = LocalTime.of(8, 0);
    private static final LocalTime ADMIN_END = LocalTime.of(17, 0);
    private static final LocalTime OUTSIDE_START = LocalTime.of(17, 0);
    private static final LocalTime OUTSIDE_END = LocalTime.of(22, 0);

    public Evaluation evaluate(
            List<SelectedCluster> route,
            GeoPoint hub,
            LocalDateTime plannedStartAt,
            LocalDateTime shiftEndAt,
            BigDecimal capacityWeight,
            BigDecimal capacityVolume,
            TravelTimeProvider travelTimeProvider
    ) {
        BigDecimal totalWeight = route.stream().map(SelectedCluster::totalWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVolume = route.stream().map(SelectedCluster::totalVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(capacityWeight) > 0 || totalVolume.compareTo(capacityVolume) > 0) {
            return Evaluation.infeasible("CAPACITY_EXCEEDED");
        }

        List<StopTiming> timings = new ArrayList<>();
        GeoPoint current = hub;
        LocalDateTime departure = plannedStartAt;
        BigDecimal remainingWeight = totalWeight;
        BigDecimal remainingVolume = totalVolume;
        for (SelectedCluster cluster : route) {
            LocalDateTime arrival = departure.plus(
                    travelTimeProvider.getTravelTime(current, cluster.location(), departure));
            LocalDateTime serviceStart = serviceStart(arrival, cluster.deliveryMethod(),
                    cluster.deliveryWindow());
            LocalDateTime clusterDeparture = serviceStart.plusMinutes(
                    10L + cluster.orders().size());
            if (clusterDeparture.isAfter(windowEnd(arrival, cluster.deliveryMethod(),
                    cluster.deliveryWindow()))) {
                return Evaluation.infeasible("DELIVERY_WINDOW_VIOLATION");
            }
            remainingWeight = remainingWeight.subtract(cluster.totalWeight());
            remainingVolume = remainingVolume.subtract(cluster.totalVolume());
            timings.add(new StopTiming(arrival, serviceStart, clusterDeparture));
            current = cluster.location();
            departure = clusterDeparture;
        }
        LocalDateTime returnAt = departure.plus(travelTimeProvider.getTravelTime(current, hub, departure));
        if (returnAt.isAfter(shiftEndAt)) {
            return Evaluation.infeasible("SHIFT_END_EXCEEDED");
        }
        return new Evaluation(true, returnAt, timings, remainingWeight, remainingVolume, null);
    }

    private LocalDateTime serviceStart(
            LocalDateTime arrival,
            DeliveryMethod method,
            DeliveryWindow window
    ) {
        if (method == DeliveryMethod.EXPRESS) {
            return arrival;
        }
        LocalTime start = window == DeliveryWindow.ADMIN ? ADMIN_START : OUTSIDE_START;
        return arrival.toLocalTime().isBefore(start)
                ? LocalDateTime.of(arrival.toLocalDate(), start)
                : arrival;
    }

    private LocalDateTime windowEnd(
            LocalDateTime arrival,
            DeliveryMethod method,
            DeliveryWindow window
    ) {
        if (method == DeliveryMethod.EXPRESS) {
            return LocalDateTime.MAX;
        }
        LocalTime end = window == DeliveryWindow.ADMIN ? ADMIN_END : OUTSIDE_END;
        return LocalDateTime.of(arrival.toLocalDate(), end);
    }

    public record StopTiming(
            LocalDateTime arrivalAt,
            LocalDateTime serviceStartAt,
            LocalDateTime departureAt
    ) { }

    public record Evaluation(
            boolean feasible,
            LocalDateTime returnToHubAt,
            List<StopTiming> timings,
            BigDecimal remainingWeight,
            BigDecimal remainingVolume,
            String reason
    ) {
        static Evaluation infeasible(String reason) {
            return new Evaluation(false, null, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, reason);
        }
    }
}
