package com.karnofleet;

import com.karnofleet.component.LastMileOrderSelector;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.LastMileOrder;
import com.karnofleet.dto.request.LastMileOrder.DeliveryMethod;
import com.karnofleet.dto.request.LastMileOrder.DeliveryWindow;
import com.karnofleet.dto.response.LastMileClustersResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LastMileOrderSelectorTest {
    private final LastMileOrderSelector selector = new LastMileOrderSelector();
    private final OffsetDateTime baseTime = OffsetDateTime.parse("2026-10-14T08:00:00+07:00");

    @Test
    void ranksExpressClustersBeforeStandardByEarliestOrder() {
        LastMileOrder expressOld = order("E-OLD", DeliveryMethod.EXPRESS, null, 1, 2);
        LastMileOrder expressNew = order("E-NEW", DeliveryMethod.EXPRESS, null, 2, 2);
        LastMileOrder standardOld = order("S-OLD", DeliveryMethod.STANDARD, DeliveryWindow.ADMIN, 1, 2);

        LastMileClustersResponse clusters = new LastMileClustersResponse(
                200,
                3,
                List.of(new LastMileClustersResponse.ExpressCluster(
                        "EXPRESS-1", expressOld.location(), List.of(expressNew, expressOld))),
                List.of(new LastMileClustersResponse.StandardCluster(
                        "STANDARD-1", standardOld.location(),
                        List.of(new LastMileClustersResponse.WindowGroup(
                                DeliveryWindow.ADMIN, List.of(standardOld))))));

        var result = selector.select(clusters, new BigDecimal("3"), new BigDecimal("5"));

        assertEquals(new BigDecimal("4"), result.totalWeight());
        assertEquals(new BigDecimal("6"), result.totalVolume());
        assertEquals(List.of("E-OLD", "E-NEW"), result.clusters().getFirst().orders()
                .stream().map(LastMileOrder::orderId).toList());
        assertEquals(List.of("EXPRESS-1", "STANDARD-1-ADMIN"),
                result.clusters().stream().map(cluster -> cluster.clusterId()).toList());
        assertEquals(expressOld.location(), result.clusters().getFirst().location());
        assertEquals(standardOld.location(), result.clusters().get(1).location());
    }

    @Test
    void splitsStandardClusterByDeliveryWindow() {
        LastMileOrder first = order("A", DeliveryMethod.STANDARD, DeliveryWindow.ADMIN, 2, 2);
        LastMileOrder outside = order("B", DeliveryMethod.STANDARD, DeliveryWindow.OUTSIDE, 4, 2);

        LastMileClustersResponse clusters = new LastMileClustersResponse(
                200,
                2,
                List.of(),
                List.of(new LastMileClustersResponse.StandardCluster(
                        "STANDARD-1", first.location(),
                        List.of(new LastMileClustersResponse.WindowGroup(
                                DeliveryWindow.ADMIN, List.of(first)),
                        new LastMileClustersResponse.WindowGroup(
                                DeliveryWindow.OUTSIDE, List.of(outside))))));

        var result = selector.select(clusters, new BigDecimal("3"), new BigDecimal("3"));

        assertEquals(List.of("STANDARD-1-ADMIN", "STANDARD-1-OUTSIDE"),
                result.clusters().stream().map(cluster -> cluster.clusterId()).toList());
        assertEquals(List.of(DeliveryWindow.ADMIN, DeliveryWindow.OUTSIDE),
                result.clusters().stream().map(cluster -> cluster.deliveryWindow()).toList());
        assertEquals(first.location(), result.clusters().getFirst().location());
        assertEquals(first.location(), result.clusters().get(1).location());
        assertEquals(new BigDecimal("6"), result.totalWeight());
        assertEquals(new BigDecimal("4"), result.totalVolume());
    }

    private LastMileOrder order(
            String id,
            DeliveryMethod method,
            DeliveryWindow window,
            int weight,
            int volume
    ) {
        return new LastMileOrder(
                id,
                method,
                window,
                new GeoPoint(BigDecimal.valueOf(21), BigDecimal.valueOf(105)),
                BigDecimal.valueOf(weight),
                BigDecimal.valueOf(volume),
                baseTime.plusMinutes(id.equals("E-OLD") || id.equals("A") ? 0 : 1));
    }
}
