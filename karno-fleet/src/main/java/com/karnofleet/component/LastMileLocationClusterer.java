package com.karnofleet.component;

import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.LastMileOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import org.springframework.stereotype.Component;

/** Complete-linkage clustering. Each output cluster has diameter <= the threshold. */
@Component
public class LastMileLocationClusterer {
    private record Pair(int left, int right, double distance) { }

    public List<List<LastMileOrder>> cluster(List<LastMileOrder> input, double threshold) {
        if (!Double.isFinite(threshold) || threshold <= 0) {
            throw new IllegalArgumentException("maxDiameterMeters must be positive and finite");
        }
        List<LastMileOrder> sorted = input.stream()
                .sorted(Comparator.comparing(LastMileOrder::orderId)).toList();
        int n = sorted.size();
        if (n == 0) {
            return List.of();
        }
        double[][] distances = new double[2 * n][2 * n];
        boolean[] active = new boolean[2 * n];
        List<List<LastMileOrder>> clusters = new ArrayList<>();
        PriorityQueue<Pair> pairs = new PriorityQueue<>(Comparator.comparingDouble(Pair::distance)
                .thenComparingInt(Pair::left).thenComparingInt(Pair::right));
        for (int i = 0; i < n; i++) {
            clusters.add(List.of(sorted.get(i)));
            active[i] = true;
            for (int j = 0; j < i; j++) {
                double distance = distanceMeters(sorted.get(i).location(), sorted.get(j).location());
                distances[i][j] = distances[j][i] = distance;
                if (distance <= threshold) {
                    pairs.add(new Pair(j, i, distance));
                }
            }
        }
        while (!pairs.isEmpty()) {
            Pair pair = pairs.remove();
            if (!active[pair.left()] || !active[pair.right()]) {
                continue;
            }
            int mergedIndex = clusters.size();
            List<LastMileOrder> merged = new ArrayList<>(clusters.get(pair.left()));
            merged.addAll(clusters.get(pair.right()));
            clusters.add(merged);
            active[pair.left()] = active[pair.right()] = false;
            active[mergedIndex] = true;
            for (int i = 0; i < mergedIndex; i++) {
                if (active[i]) {
                    double distance = Math.max(distances[pair.left()][i], distances[pair.right()][i]);
                    distances[mergedIndex][i] = distances[i][mergedIndex] = distance;
                    if (distance <= threshold) {
                        pairs.add(new Pair(i, mergedIndex, distance));
                    }
                }
            }
        }
        List<List<LastMileOrder>> result = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            if (active[i]) {
                result.add(clusters.get(i).stream()
                        .sorted(Comparator.comparing(LastMileOrder::orderId)).toList());
            }
        }
        result.sort(Comparator.comparing(cluster -> cluster.getFirst().orderId()));
        return List.copyOf(result);
    }

    public static double distanceMeters(GeoPoint from, GeoPoint to) {
        double lat1 = Math.toRadians(from.latitude().doubleValue());
        double lat2 = Math.toRadians(to.latitude().doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(to.longitude().doubleValue() - from.longitude().doubleValue());
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLon / 2), 2);
        return 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, a))));
    }
}
