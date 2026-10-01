package com.karnofleet.dto.response;

import java.time.Duration;

public record RouteGeometryResponse(
        String encodedPolyline,
        long distanceMeters,
        Duration travelDuration
) {
}
