package com.karnofleet.dto.response;

import java.util.List;

public record OsrmRouteResponse(
        String code,
        List<Route> routes
) {
    public record Route(
            double distance,
            double duration,
            String geometry
    ){}
}
