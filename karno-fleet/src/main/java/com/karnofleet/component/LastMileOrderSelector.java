package com.karnofleet.component;

import com.karnofleet.dto.request.LastMileOrder;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.LastMileOrder.DeliveryMethod;
import com.karnofleet.dto.request.LastMileOrder.DeliveryWindow;
import com.karnofleet.dto.response.LastMileClustersResponse;
import com.karnofleet.dto.response.LastMileLoadResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LastMileOrderSelector {

    public LastMileLoadResponse select(
            LastMileClustersResponse clusters,
            BigDecimal capacityWeight,
            BigDecimal capacityVolume
    ) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        List<LastMileLoadResponse.SelectedCluster> selectedClusters = new ArrayList<>();

        for (DeliveryMethod method : List.of(DeliveryMethod.EXPRESS, DeliveryMethod.STANDARD)) {
            for (ClusterOrders cluster : clustersFor(clusters, method)) {
                for (LastMileOrder order : cluster.orders()) {
                    totalWeight = totalWeight.add(order.weightKg());
                    totalVolume = totalVolume.add(order.volumeM3());
                }
                selectedClusters.add(new LastMileLoadResponse.SelectedCluster(
                        cluster.clusterId(), method, cluster.deliveryWindow(),
                        cluster.location(), cluster.totalWeight(), cluster.totalVolume(), cluster.orders()));
            }
        }

        return new LastMileLoadResponse(
                capacityWeight,
                capacityVolume,
                totalWeight,
                totalVolume,
                List.copyOf(selectedClusters));
    }

    private List<ClusterOrders> clustersFor(
            LastMileClustersResponse clusters,
            DeliveryMethod method
    ) {
        List<ClusterOrders> result = new ArrayList<>();
        if (method == DeliveryMethod.EXPRESS) {
            clusters.express().forEach(cluster -> result.add(
                    new ClusterOrders(
                            cluster.clusterId(), null, cluster.location(), sortedOrders(cluster.orders()))));
        } else {
            clusters.standard().forEach(cluster -> cluster.groups().forEach(group -> {
                result.add(new ClusterOrders(
                        cluster.clusterId() + "-" + group.deliveryWindow(),
                        group.deliveryWindow(),
                        cluster.location(),
                        sortedOrders(group.orders())));
            }));
        }
        result.sort(Comparator
                .comparing((ClusterOrders cluster) -> cluster.orders().getFirst().createdAt())
                .thenComparing(cluster -> cluster.orders().getFirst().orderId()));
        return result;
    }

    private List<LastMileOrder> sortedOrders(List<LastMileOrder> orders) {
        return orders.stream()
                .sorted(Comparator.comparing(LastMileOrder::createdAt)
                        .thenComparing(LastMileOrder::orderId))
                .toList();
    }

    private record ClusterOrders(
            String clusterId,
            DeliveryWindow deliveryWindow,
            GeoPoint location,
            List<LastMileOrder> orders
    ) {
        private BigDecimal totalWeight() {
            return orders.stream().map(LastMileOrder::weightKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal totalVolume() {
            return orders.stream().map(LastMileOrder::volumeM3)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
