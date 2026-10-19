package com.karnofleet.dto.response;

import com.karnofleet.dto.request.LastMileOrder;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.LastMileOrder.DeliveryWindow;
import java.util.List;

public record LastMileClustersResponse(
        double maxDiameterMeters,
        int totalOrders,
        List<ExpressCluster> express,
        List<StandardCluster> standard
) {
    public record ExpressCluster(String clusterId, GeoPoint location, List<LastMileOrder> orders) { }
    public record StandardCluster(String clusterId, GeoPoint location, List<WindowGroup> groups) { }
    public record WindowGroup(DeliveryWindow deliveryWindow, List<LastMileOrder> orders) { }
}
