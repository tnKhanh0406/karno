package com.karnofleet.dto.response;

import com.karnofleet.dto.request.LastMileOrder;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.LastMileOrder.DeliveryMethod;
import com.karnofleet.dto.request.LastMileOrder.DeliveryWindow;
import java.math.BigDecimal;
import java.util.List;

public record LastMileLoadResponse(
        BigDecimal capacityWeight,
        BigDecimal capacityVolume,
        BigDecimal totalWeight,
        BigDecimal totalVolume,
        List<SelectedCluster> clusters
) {
    public record SelectedCluster(
            String clusterId,
            DeliveryMethod deliveryMethod,
            DeliveryWindow deliveryWindow,
            GeoPoint location,
            BigDecimal totalWeight,
            BigDecimal totalVolume,
            List<LastMileOrder> orders
    ) { }
}
