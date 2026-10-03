package com.karnofleet.dto.response;

import com.karnofleet.enums.PolylineFormat;

import java.time.Duration;

public record RouteGeometryResponse(
        String encodedPolyline,
        PolylineFormat polylineFormat,
        long distanceMeters,
        Duration travelDuration
) {
}
