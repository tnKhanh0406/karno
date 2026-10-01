package com.karnofleet.component;

import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.RouteKey;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

public class MatrixTravelTimeProvider implements TravelTimeProvider {
    private final Map<RouteKey, Duration> matrix;

    public MatrixTravelTimeProvider(Map<RouteKey, Duration> matrix) {
        this.matrix = matrix;
    }

    @Override
    public Duration getTravelTime(GeoPoint from, GeoPoint to, LocalDateTime departureAt) {
        RouteKey key = RouteKey.of(from, to);
        Duration duration = matrix.get(key);
        if (duration == null) {
            throw new IllegalStateException("Missing travel time: " + from + " -> " + to);
        }
        return duration;
    }
}
