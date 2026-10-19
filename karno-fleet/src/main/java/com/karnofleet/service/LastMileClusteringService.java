package com.karnofleet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnofleet.component.LastMileLocationClusterer;
import com.karnofleet.dto.request.LastMileOrder;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.LastMileOrder.DeliveryMethod;
import com.karnofleet.dto.request.LastMileOrder.DeliveryWindow;
import com.karnofleet.dto.response.LastMileClustersResponse;
import com.karnofleet.dto.response.LastMileClustersResponse.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class LastMileClusteringService {
    private final LastMileLocationClusterer clusterer;
    private final List<LastMileOrder> demoOrders;

    public LastMileClusteringService(LastMileLocationClusterer clusterer, ObjectMapper objectMapper)
            throws IOException {
        this.clusterer = clusterer;
        try (var stream = new ClassPathResource("fixtures/last-mile-orders.json").getInputStream()) {
            demoOrders = List.copyOf(objectMapper.readValue(stream, new TypeReference<List<LastMileOrder>>() { }));
        }
    }

    public LastMileClustersResponse demo(double maxDiameterMeters) {
        return cluster(demoOrders, maxDiameterMeters);
    }

    public LastMileClustersResponse cluster(List<LastMileOrder> orders, double maxDiameterMeters) {
        validate(orders);
        var express = clusterer.cluster(orders.stream()
                .filter(order -> order.deliveryMethod() == DeliveryMethod.EXPRESS).toList(), maxDiameterMeters);
        var standard = clusterer.cluster(orders.stream()
                .filter(order -> order.deliveryMethod() == DeliveryMethod.STANDARD).toList(), maxDiameterMeters);
        List<ExpressCluster> expressResult = new ArrayList<>();
        for (int i = 0; i < express.size(); i++) {
            expressResult.add(new ExpressCluster("EXPRESS-" + (i + 1),
                    representativeLocation(express.get(i)), express.get(i)));
        }
        List<StandardCluster> standardResult = new ArrayList<>();
        for (int i = 0; i < standard.size(); i++) {
            List<WindowGroup> groups = new ArrayList<>();
            for (DeliveryWindow window : DeliveryWindow.values()) {
                var members = standard.get(i).stream().filter(order -> order.deliveryWindow() == window).toList();
                if (!members.isEmpty()) {
                    groups.add(new WindowGroup(window, members));
                }
            }
            standardResult.add(new StandardCluster("STANDARD-" + (i + 1),
                    representativeLocation(standard.get(i)), List.copyOf(groups)));
        }
        return new LastMileClustersResponse(maxDiameterMeters, orders.size(),
                List.copyOf(expressResult), List.copyOf(standardResult));
    }

    /** Medoid: an actual delivery point minimizing total geographic distance to the orders.
     * Duplicate locations count per order; equal totals are resolved by order ID.
     * This is a routing approximation, not a verified vehicle access point.
     */
    private GeoPoint representativeLocation(List<LastMileOrder> orders) {
        LastMileOrder best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (LastMileOrder candidate : orders) {
            double totalDistance = 0;
            for (LastMileOrder order : orders) {
                totalDistance += LastMileLocationClusterer.distanceMeters(candidate.location(), order.location());
            }
            if (best == null || totalDistance < bestDistance
                    || (Double.compare(totalDistance, bestDistance) == 0
                    && candidate.orderId().compareTo(best.orderId()) < 0)) {
                best = candidate;
                bestDistance = totalDistance;
            }
        }
        return best.location();
    }

    private void validate(List<LastMileOrder> orders) {
        if (orders == null || orders.size() > 1000) {
            throw new IllegalArgumentException("Provide at most 1000 orders per demo request");
        }
        var ids = new HashSet<String>();
        for (LastMileOrder order : orders) {
            if (order == null || order.orderId() == null || order.orderId().isBlank()
                    || !ids.add(order.orderId()) || order.deliveryMethod() == null
                    || order.location() == null || order.location().latitude() == null
                    || order.location().longitude() == null
                    || order.location().latitude().abs().doubleValue() > 90
                    || order.location().longitude().abs().doubleValue() > 180
                    || order.createdAt() == null || order.weightKg() == null || order.weightKg().signum() <= 0
                    || order.volumeM3() == null || order.volumeM3().signum() <= 0) {
                throw new IllegalArgumentException("Orders require unique IDs, valid coordinates, time and positive load");
            }
            if ((order.deliveryMethod() == DeliveryMethod.EXPRESS && order.deliveryWindow() != null)
                    || (order.deliveryMethod() == DeliveryMethod.STANDARD && order.deliveryWindow() == null)) {
                throw new IllegalArgumentException("EXPRESS must have no window; STANDARD requires ADMIN or OUTSIDE");
            }
        }
    }
}
