package com.karnofleet.component;

import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.RouteKey;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class TestTravelMatrix {
    private final Map<RouteKey, Duration> matrix = new HashMap<>();

    TestTravelMatrix add(
            GeoPoint from,
            GeoPoint to,
            long minutes
    ) {

        matrix.put(
                RouteKey.of(from, to),
                Duration.ofMinutes(minutes)
        );

        return this;
    }

    public MatrixTravelTimeProvider build() {
        return new MatrixTravelTimeProvider(matrix);
    }
}
