package com.karnofleet.dto.response;

import lombok.Builder;

@Builder
public record RouteGeometry(
        String encodedPolyline,
        long distanceMeters,
        long durationSeconds
){
}
