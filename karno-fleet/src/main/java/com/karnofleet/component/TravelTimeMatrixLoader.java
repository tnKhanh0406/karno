package com.karnofleet.component;

import com.karnofleet.dto.request.GeoPoint;

import java.time.LocalDateTime;
import java.util.List;

public interface TravelTimeMatrixLoader {
    TravelTimeProvider load(List<GeoPoint> points, LocalDateTime plannedAt);
}
