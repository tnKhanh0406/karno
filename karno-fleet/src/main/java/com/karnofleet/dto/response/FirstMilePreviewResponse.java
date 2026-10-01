package com.karnofleet.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record FirstMilePreviewResponse(
        RouteSummaryResponse route,
        List<PickupStopResponse> stops,
        List<UnassignedRequestResponse> unassigned,
        RouteGeometry geometry
) {}
