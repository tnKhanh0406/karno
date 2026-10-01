package com.karnofleet.component;

import com.karnofleet.dto.request.PickupStopInput;
import com.karnofleet.dto.request.PlanningRequest;
import com.karnofleet.dto.request.RouteEvaluationRequest;
import com.karnofleet.dto.response.UnassignedRequestResponse;
import com.karnofleet.dto.response.PlanningCandidate;
import com.karnofleet.dto.response.PlanningResult;
import com.karnofleet.dto.response.RouteEvaluationResult;
import com.karnofleet.enums.UnassignedReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CheapestFeasibleInsertionPlanner {

    private final RouteEvaluator routeEvaluator;
    private final PickupPriorityResolver priorityResolver;

    public PlanningResult plan(PlanningRequest request, TravelTimeProvider travelTimeProvider) {
        // Route hiện tại đang được xây dựng.
        List<PickupStopInput> currentRoute = new ArrayList<>();

        // Danh sách các request không được assign.
        List<UnassignedRequestResponse> unassigned = new ArrayList<>();

        // 1. Sắp xếp các request theo priority.
        List<PickupStopInput> prioritizedRequests = priorityResolver.sort(
                request.pickupRequests(),
                request.plannedStartAt()
        );

        // 2. Duyệt từng request theo priority.
        for (PickupStopInput pickupRequest : prioritizedRequests) {
            if (requestExceedsVehicleCapacity(pickupRequest, request)) {
                unassigned.add(
                        UnassignedRequestResponse.builder()
                                .requestId(pickupRequest.requestId())
                                .requestCode(pickupRequest.requestCode())
                                .reason(UnassignedReason.REQUEST_EXCEEDS_CAPACITY)
                                .build()
                );
                continue;
            }

            // 3. Tìm vị trí insert tốt nhất cho request này.
            Optional<PlanningCandidate> bestCandidate = findBestInsertion(
                            request,
                            currentRoute,
                            pickupRequest,
                            travelTimeProvider
            );

            // 4. Nếu tìm được vị trí insert tốt nhất, update currentRoute.
            if (bestCandidate.isPresent()) {
                currentRoute = new ArrayList<>(bestCandidate.get().route());
            } else {
                unassigned.add(
                        UnassignedRequestResponse.builder()
                                .requestId(pickupRequest.requestId())
                                .requestCode(pickupRequest.requestCode())
                                .reason(UnassignedReason.NO_FEASIBLE_INSERTION)
                                .build()
                );
            }
        }

        // 5. Sau khi duyệt hết các request, đánh giá route cuối cùng.
        RouteEvaluationResult finalEvaluation =
                routeEvaluator.evaluate(
                        toEvaluationRequest(
                                request,
                                currentRoute
                        ),
                        travelTimeProvider
                );

        return PlanningResult.builder()
                .selectedStops(List.copyOf(currentRoute))
                .unassigned(List.copyOf(unassigned))
                .routeEvaluation(finalEvaluation)
                .build();
    }

    private boolean requestExceedsVehicleCapacity(
            PickupStopInput request,
            PlanningRequest planningRequest
    ) {
        boolean weightExceeded = request.weight().compareTo(planningRequest.capacityWeight()) > 0;
        boolean volumeExceeded = request.volume().compareTo(planningRequest.capacityVolume()) > 0;
        return weightExceeded || volumeExceeded;
    }

    private RouteEvaluationRequest toEvaluationRequest(
            PlanningRequest planningRequest,
            List<PickupStopInput> route
    ) {

        return RouteEvaluationRequest.builder()
                .startLocation(planningRequest.startLocation())
                .plannedStartAt(planningRequest.plannedStartAt())
                .hubLocation(planningRequest.hubLocation())
                .shiftEndAt(planningRequest.shiftEndAt())
                .capacityWeight(planningRequest.capacityWeight())
                .capacityVolume(planningRequest.capacityVolume())
                .initialWeight(planningRequest.initialWeight())
                .initialVolume(planningRequest.initialVolume())
                .stops(route)
                .build();
    }

    private Optional<PlanningCandidate> findBestInsertion(
            PlanningRequest planningRequest,
            List<PickupStopInput> currentRoute,
            PickupStopInput requestToInsert,
            TravelTimeProvider travelTimeProvider
    ) {
        List<PlanningCandidate> feasibleCandidates = new ArrayList<>();
        /*
         * route size N
         * => N + 1 vị trí insertion.
         */
        for (int index = 0; index <= currentRoute.size(); index++) {
            /*
             * Ví dụ:
             * [A,B]
             *
             * insert X index 1
             *
             * => [A,X,B]
             */
            List<PickupStopInput> candidateRoute = insertAt(
                            currentRoute,
                            requestToInsert,
                            index
            );

            RouteEvaluationResult evaluation = routeEvaluator.evaluate(
                            toEvaluationRequest(
                                    planningRequest,
                                    candidateRoute
                            ),
                            travelTimeProvider
            );

            if (!evaluation.feasible()) {
                continue;
            }

            feasibleCandidates.add(
                    PlanningCandidate.builder()
                            .insertionIndex(index)
                            .route(candidateRoute)
                            .evaluation(evaluation)
                            .build()
            );
        }

        /*
         * Chọn:
         *
         * returnToHub sớm nhất.
         *
         * Nếu bằng nhau:
         * insertionIndex nhỏ nhất.
         */
        return feasibleCandidates.stream()
                .min(Comparator
                        .comparing(
                                (PlanningCandidate candidate) -> candidate.evaluation().returnToHubAt())
                                .thenComparingInt(PlanningCandidate::insertionIndex)
                );
    }

    private List<PickupStopInput> insertAt(
            List<PickupStopInput> currentRoute,
            PickupStopInput request,
            int index
    ) {
        List<PickupStopInput> result = new ArrayList<>(currentRoute);
        result.add(index, request);
        return result;
    }
}
