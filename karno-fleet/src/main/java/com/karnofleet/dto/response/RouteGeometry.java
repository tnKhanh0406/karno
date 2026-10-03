package com.karnofleet.dto.response;

import com.karnofleet.enums.PolylineFormat;
import lombok.Builder;

@Builder
public record RouteGeometry(
        String encodedPolyline,
        PolylineFormat polylineFormat,
        long distanceMeters,
        long durationSeconds
){
}
