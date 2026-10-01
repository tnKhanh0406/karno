package com.karnofleet.dto.response;

import com.karnofleet.dto.request.PickupStopInput;
import lombok.Builder;

import java.util.List;

@Builder
public record PlanningCandidate(
        int insertionIndex,
        List<PickupStopInput> route,
        RouteEvaluationResult evaluation
) {
}
