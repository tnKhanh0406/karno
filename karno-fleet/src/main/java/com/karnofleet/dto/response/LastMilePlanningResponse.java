package com.karnofleet.dto.response;

import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.LastMileOrder;
import com.karnofleet.dto.request.LastMileOrder.DeliveryMethod;
import com.karnofleet.dto.request.LastMileOrder.DeliveryWindow;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LastMilePlanningResponse(
        BigDecimal capacityWeight,
        BigDecimal capacityVolume,
        BigDecimal totalWeight,
        BigDecimal totalVolume,
        boolean feasible,
        LocalDateTime returnToHubAt,
        RouteGeometryResponse geometry,
        List<PlannedCluster> selectedClusters,
        List<UnassignedCluster> unassignedClusters
) {
    public record PlannedCluster(
            int sequence,
            String clusterId,
            DeliveryMethod deliveryMethod,
            DeliveryWindow deliveryWindow,
            GeoPoint location,
            BigDecimal totalWeight,
            BigDecimal totalVolume,
            LocalDateTime arrivalAt,
            LocalDateTime serviceStartAt,
            LocalDateTime departureAt,
            List<LastMileOrder> orders
    ) { }

    public record UnassignedCluster(
            String clusterId,
            String reason,
            GeoPoint location,
            List<LastMileOrder> orders
    ) { }
}
