package com.karnofleet.component;

import com.karnofleet.dto.response.*;
import org.springframework.stereotype.Component;

@Component
public class FirstMileResponseMapper {

    public FirstMilePreviewResponse toResponse(PlanningResult result){

        return FirstMilePreviewResponse.builder()
                .route(toRoute(result.routeEvaluation()))
                .stops(result.routeEvaluation()
                                .stops()
                                .stream()
                                .map(this::toStop)
                                .toList()
                )
                .unassigned(result.unassigned())
                .geometry(toGeometry(result.geometry()))
                .build();
    }

    private RouteSummaryResponse toRoute(RouteEvaluationResult route){
        return RouteSummaryResponse.builder()
                .feasible(route.feasible())
                .totalStops(route.stops().size())
                .finalWeight(route.finalWeight())
                .finalVolume(route.finalVolume())
                .returnToHubAt(route.returnToHubAt())
                .violations(route.violations())
                .build();
    }



    private PickupStopResponse toStop(EvaluatedStop stop){
        return PickupStopResponse.builder()
                .sequence(stop.sequence())
                .requestId(stop.requestId())
                .requestCode(stop.requestCode())
                .location(stop.location())
                .arrivalAt(stop.arrivalAt())
                .serviceStartAt(stop.serviceStartAt())
                .departureAt(stop.departureAt())
                .waitingTime(stop.waitingTime())
                .loadWeightAfter(stop.loadWeightAfter())
                .loadVolumeAfter(stop.loadVolumeAfter())
                .build();
    }



    private RouteGeometry toGeometry(RouteGeometryResponse geometry){
        if(geometry == null){
            return null;
        }

        return new RouteGeometry(
                geometry.encodedPolyline(),
                geometry.distanceMeters(),
                geometry.travelDuration().toSeconds()
        );
    }
}
