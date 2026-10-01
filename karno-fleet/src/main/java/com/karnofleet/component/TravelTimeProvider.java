package com.karnofleet.component;

import com.karnofleet.dto.request.GeoPoint;

import java.time.Duration;
import java.time.LocalDateTime;

public interface TravelTimeProvider {
    Duration getTravelTime(GeoPoint from, GeoPoint to, LocalDateTime departureAt);
}