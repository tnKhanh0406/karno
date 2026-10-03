package com.karnofleet.dto.response;

import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.PickupStopInput;
import lombok.Builder;

import java.util.List;

@Builder
public record PlanningResult(
        List<PickupStopInput> selectedStops,
        List<UnassignedRequestResponse> unassigned,
        RouteEvaluationResult routeEvaluation,
        RouteGeometryResponse geometry,
        GeoPoint hub
) {
}
