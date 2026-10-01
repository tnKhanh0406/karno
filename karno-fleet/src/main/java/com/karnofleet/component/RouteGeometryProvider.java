package com.karnofleet.component;

import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.response.RouteGeometryResponse;

import java.util.List;

public interface RouteGeometryProvider {
    RouteGeometryResponse compute(
            GeoPoint start,
            List<GeoPoint> orderedStops,
            GeoPoint hub
    );
}