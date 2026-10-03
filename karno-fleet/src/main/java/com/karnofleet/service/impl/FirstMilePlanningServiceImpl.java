package com.karnofleet.service.impl;

import com.karnofleet.component.*;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.LocationRequest;
import com.karnofleet.dto.request.PickupStopInput;
import com.karnofleet.dto.request.PlanningRequest;
import com.karnofleet.dto.response.FirstMileContext;
import com.karnofleet.dto.response.FirstMilePreviewResponse;
import com.karnofleet.dto.response.PlanningResult;
import com.karnofleet.dto.response.RouteGeometryResponse;
import com.karnofleet.entity.PickupRequest;
import com.karnofleet.enums.PickupRequestStatus;
import com.karnofleet.repository.PickupRequestRepo;
import com.karnofleet.service.FirstMileContextService;
import com.karnofleet.service.FirstMilePlanningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirstMilePlanningServiceImpl implements FirstMilePlanningService {

    private final FirstMileContextService contextService;
    private final PickupRequestRepo pickupRequestRepo;
    private final CheapestFeasibleInsertionPlanner planner;
    private final TravelTimeMatrixLoader matrixLoader;
    private final RouteGeometryProvider routeGeometryProvider;
    private final FirstMileResponseMapper mapper;
    private final Clock clock;

    @Override
    public FirstMilePreviewResponse preview(UUID shipperUserId, LocationRequest request) {
        LocalDateTime plannedAt = LocalDateTime.now(clock);

        FirstMileContext context = contextService.getContext(shipperUserId, plannedAt);

        List<PickupRequest> pickupRequests = pickupRequestRepo.findPlanningCandidates(context.hub().id(), PickupRequestStatus.WAITING_PLANNING);

        List<PickupStopInput> stops = pickupRequests.stream()
                        .map(this::toStop)
                        .toList();

        GeoPoint start = new GeoPoint(request.latitude(), request.longitude());
        GeoPoint hub = new GeoPoint(context.hub().latitude(), context.hub().longitude());

        List<GeoPoint> matrixPoints = buildMatrixPoints(
                start,
                stops,
                hub
        );

        log.info(
                "Matrix loader = {}",
                matrixLoader.getClass().getName()
        );
        TravelTimeProvider provider = matrixLoader.load(matrixPoints, plannedAt);
        logMatrix(
                provider,
                matrixPoints,
                stops,
                hub,
                plannedAt
        );

        PlanningRequest planningRequest = PlanningRequest.builder()
                        .startLocation(start)
                        .plannedStartAt(plannedAt)
                        .hubLocation(hub)
                        .shiftEndAt(context.shiftEndsAt())
                        .capacityWeight(context.vehicle().getCapacityWeight())
                        .capacityVolume(context.vehicle().getCapacityVolume())
                        .initialWeight(BigDecimal.ZERO)
                        .initialVolume(BigDecimal.ZERO)
                        .pickupRequests(stops)
                        .build();
        log.info(
                "TravelTime provider = {}",
                provider.getClass().getName()
        );
        PlanningResult result = planner.plan(planningRequest, provider);

        RouteGeometryResponse geometry = routeGeometryProvider.compute(
                        start,
                        result.selectedStops()
                                .stream()
                                .map(PickupStopInput::location)
                                .toList(),
                        hub,
                        plannedAt
        );
        return mapper.toResponse(PlanningResult.builder()
                .selectedStops(result.selectedStops())
                .unassigned(result.unassigned())
                .routeEvaluation(result.routeEvaluation())
                .geometry(geometry)
                .hub(hub)
                .build());
    }

    private PickupStopInput toStop(PickupRequest request) {
        return PickupStopInput.builder()
                .requestId(request.getId())
                .requestCode(request.getRequestCode())
                .location(
                        new GeoPoint(
                                request.getPickupLatitude(),
                                request.getPickupLongitude()
                        )
                )
                .weight(request.getTotalWeight())
                .volume(request.getTotalVolume())
                .windowStart(request.getPickupWindowStart())
                .windowEnd(request.getPickupWindowEnd())
                .serviceDurationSeconds(request.getServiceDurationSeconds())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private List<GeoPoint> buildMatrixPoints(
            GeoPoint startLocation,
            List<PickupStopInput> stops,
            GeoPoint hubLocation
    ) {

        List<GeoPoint> points = new ArrayList<>();
        points.add(startLocation);
        stops.forEach(
                stop -> points.add(stop.location())
        );

        points.add(hubLocation);
        return points;
    }

    private void logMatrix(
            TravelTimeProvider provider,
            List<GeoPoint> points,
            List<PickupStopInput> stops,
            GeoPoint hub,
            LocalDateTime departureAt
    ) {
        List<String> labels = new ArrayList<>();

        labels.add("START");

        stops.forEach(
                stop -> labels.add(stop.requestCode())
        );

        labels.add("HUB");


        log.info("========== TRAVEL TIME MATRIX ==========");


        // Header
        StringBuilder header = new StringBuilder();

        header.append(String.format("%-15s", ""));

        for (String label : labels) {
            header.append(
                    String.format("%-12s", label)
            );
        }

        log.info(header.toString());


        // Matrix
        for (int i = 0; i < points.size(); i++) {

            StringBuilder row = new StringBuilder();

            row.append(
                    String.format(
                            "%-15s",
                            labels.get(i)
                    )
            );


            for (int j = 0; j < points.size(); j++) {

                if (i == j) {

                    row.append(
                            String.format(
                                    "%-12s",
                                    "0"
                            )
                    );

                    continue;
                }


                Duration duration =
                        provider.getTravelTime(
                                points.get(i),
                                points.get(j),
                                departureAt
                        );


                row.append(
                        String.format(
                                "%-12s",
                                formatDuration(duration)
                        )
                );
            }


            log.info(row.toString());
        }


        log.info("========================================");
    }

    private String formatDuration(Duration duration) {

        long minutes =
                duration.toSeconds() / 60;

        long seconds =
                duration.toSeconds() % 60;

        return minutes + "m " + seconds + "s";
    }
}
