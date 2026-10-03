package com.karnofleet.dto.request;

import java.util.List;

public record HereMatrixRequest(
        List<HereLocation> origins,
        List<HereLocation> destinations,
        HereRegionDefinition regionDefinition,
        String departureTime,
        String routingMode,
        String transportMode,
        HereVehicle vehicle,
        List<String> matrixAttributes
) {
}
