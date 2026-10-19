package com.karnofleet.service;

import com.karnofleet.component.LastMileCheapestFeasibleInsertionPlanner;
import com.karnofleet.component.RouteGeometryProvider;
import com.karnofleet.component.TravelTimeMatrixLoader;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.LastMileOrder;
import com.karnofleet.dto.response.FirstMileContext;
import com.karnofleet.dto.response.LastMileLoadResponse.SelectedCluster;
import com.karnofleet.dto.response.LastMilePlanningResponse;
import com.karnofleet.dto.response.RouteGeometryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LastMilePlanningService {
    private final FirstMileContextService contextService;
    private final LastMileLoadingService loadingService;
    private final TravelTimeMatrixLoader matrixLoader;
    private final LastMileCheapestFeasibleInsertionPlanner planner;
    private final RouteGeometryProvider routeGeometryProvider;

    public LastMilePlanningResponse plan(
            UUID shipperUserId,
            List<LastMileOrder> orders,
            double maxDiameterMeters,
            LocalDateTime plannedStartAt
    ) {
        FirstMileContext context = contextService.getContext(shipperUserId, plannedStartAt);
        LastMileLoadResponseWrapper sorted = sorted(
                shipperUserId, orders, maxDiameterMeters);
        return planSorted(context, sorted.clusters(), plannedStartAt);
    }

    public LastMilePlanningResponse planDemo(
            UUID shipperUserId,
            double maxDiameterMeters,
            LocalDateTime plannedStartAt
    ) {
        FirstMileContext context = contextService.getContext(shipperUserId, plannedStartAt);
        LastMileLoadResponseWrapper sorted = sortedDemo(
                shipperUserId, maxDiameterMeters);
        return planSorted(context, sorted.clusters(), plannedStartAt);
    }

    private LastMilePlanningResponse planSorted(
            FirstMileContext context,
            List<SelectedCluster> sortedClusters,
            LocalDateTime plannedStartAt
    ) {
        GeoPoint hub = new GeoPoint(context.hub().latitude(), context.hub().longitude());
        List<GeoPoint> points = new ArrayList<>();
        points.add(hub);
        sortedClusters.forEach(cluster -> points.add(cluster.location()));
        TravelTimeProviderHolder provider = new TravelTimeProviderHolder(
                matrixLoader.load(points, plannedStartAt));

        var result = planner.plan(
                sortedClusters,
                hub,
                plannedStartAt,
                context.shiftEndsAt(),
                context.vehicle().getCapacityWeight(),
                context.vehicle().getCapacityVolume(),
                provider.provider());
        RouteGeometryResponse geometry = routeGeometryProvider.compute(
                hub,
                result.route().stream().map(SelectedCluster::location).toList(),
                hub,
                plannedStartAt);
        List<LastMilePlanningResponse.PlannedCluster> selected = new ArrayList<>();
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        for (int i = 0; i < result.route().size(); i++) {
            SelectedCluster cluster = result.route().get(i);
            var timing = result.evaluation().timings().get(i);
            totalWeight = totalWeight.add(cluster.totalWeight());
            totalVolume = totalVolume.add(cluster.totalVolume());
            selected.add(new LastMilePlanningResponse.PlannedCluster(
                    i + 1, cluster.clusterId(), cluster.deliveryMethod(),
                    cluster.deliveryWindow(), cluster.location(),
                    cluster.totalWeight(), cluster.totalVolume(),
                    timing.arrivalAt(), timing.serviceStartAt(), timing.departureAt(),
                    cluster.orders()));
        }
        return new LastMilePlanningResponse(
                context.vehicle().getCapacityWeight(),
                context.vehicle().getCapacityVolume(),
                totalWeight,
                totalVolume,
                result.evaluation().feasible(),
                result.evaluation().returnToHubAt(),
                geometry,
                List.copyOf(selected),
                result.unassigned());
    }

    private LastMileLoadResponseWrapper sorted(
            UUID shipperUserId,
            List<LastMileOrder> orders,
            double maxDiameterMeters
    ) {
        return new LastMileLoadResponseWrapper(
                loadingService.select(shipperUserId, orders, maxDiameterMeters).clusters());
    }

    private LastMileLoadResponseWrapper sortedDemo(UUID shipperUserId, double maxDiameterMeters) {
        return new LastMileLoadResponseWrapper(
                loadingService.selectDemo(shipperUserId, maxDiameterMeters).clusters());
    }

    private record LastMileLoadResponseWrapper(List<SelectedCluster> clusters) { }
    private record TravelTimeProviderHolder(com.karnofleet.component.TravelTimeProvider provider) { }
}
