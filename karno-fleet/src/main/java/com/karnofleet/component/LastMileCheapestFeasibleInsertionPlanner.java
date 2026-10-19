package com.karnofleet.component;

import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.LastMileOrder.DeliveryMethod;
import com.karnofleet.dto.response.LastMileLoadResponse.SelectedCluster;
import com.karnofleet.dto.response.LastMilePlanningResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LastMileCheapestFeasibleInsertionPlanner {
    private final LastMileRouteEvaluator evaluator;

    public LastMileCheapestFeasibleInsertionPlanner(LastMileRouteEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public Result plan(
            List<SelectedCluster> prioritizedClusters,
            GeoPoint hub,
            LocalDateTime plannedStartAt,
            LocalDateTime shiftEndAt,
            BigDecimal capacityWeight,
            BigDecimal capacityVolume,
            TravelTimeProvider travelTimeProvider
    ) {
        List<SelectedCluster> route = new ArrayList<>();
        List<LastMilePlanningResponse.UnassignedCluster> unassigned = new ArrayList<>();
        boolean expressBlocked = false;
        for (SelectedCluster cluster : prioritizedClusters) {
            if (cluster.deliveryMethod() == DeliveryMethod.STANDARD && expressBlocked) {
                unassigned.add(unassigned(cluster, "EXPRESS_PRECEDENCE_BLOCKED"));
                continue;
            }
            if (cluster.totalWeight().compareTo(capacityWeight) > 0
                    || cluster.totalVolume().compareTo(capacityVolume) > 0) {
                unassigned.add(unassigned(cluster, "CAPACITY_EXCEEDED"));
                if (cluster.deliveryMethod() == DeliveryMethod.EXPRESS) {
                    expressBlocked = true;
                }
                continue;
            }
            List<Candidate> candidates = new ArrayList<>();
            int firstAllowedIndex = firstAllowedInsertionIndex(route, cluster);
            for (int index = firstAllowedIndex; index <= route.size(); index++) {
                List<SelectedCluster> candidateRoute = new ArrayList<>(route);
                candidateRoute.add(index, cluster);
                LastMileRouteEvaluator.Evaluation evaluation = evaluator.evaluate(
                        candidateRoute, hub, plannedStartAt, shiftEndAt,
                        capacityWeight, capacityVolume, travelTimeProvider);
                if (evaluation.feasible()) {
                    candidates.add(new Candidate(index, candidateRoute, evaluation));
                }
            }
            Candidate bestCandidate = candidates.stream()
                    .min(Comparator.comparing((Candidate candidate) -> candidate.evaluation().returnToHubAt())
                            .thenComparingInt(Candidate::index))
                    .orElse(null);
            if (bestCandidate == null) {
                unassigned.add(unassigned(cluster, "NO_FEASIBLE_INSERTION"));
                if (cluster.deliveryMethod() == DeliveryMethod.EXPRESS) {
                    expressBlocked = true;
                }
            } else {
                route = bestCandidate.route();
            }
        }
        LastMileRouteEvaluator.Evaluation finalEvaluation = evaluator.evaluate(
                route, hub, plannedStartAt, shiftEndAt,
                capacityWeight, capacityVolume, travelTimeProvider);
        return new Result(List.copyOf(route), List.copyOf(unassigned), finalEvaluation);
    }

    private int firstAllowedInsertionIndex(
            List<SelectedCluster> route,
            SelectedCluster cluster
    ) {
        if (cluster.deliveryMethod() == DeliveryMethod.EXPRESS) {
            return 0;
        }
        int firstStandardIndex = 0;
        while (firstStandardIndex < route.size()
                && route.get(firstStandardIndex).deliveryMethod() == DeliveryMethod.EXPRESS) {
            firstStandardIndex++;
        }
        return firstStandardIndex;
    }

    private LastMilePlanningResponse.UnassignedCluster unassigned(
            SelectedCluster cluster,
            String reason
    ) {
        return new LastMilePlanningResponse.UnassignedCluster(
                cluster.clusterId(), reason, cluster.location(), cluster.orders());
    }

    private record Candidate(
            int index,
            List<SelectedCluster> route,
            LastMileRouteEvaluator.Evaluation evaluation
    ) { }

    public record Result(
            List<SelectedCluster> route,
            List<LastMilePlanningResponse.UnassignedCluster> unassigned,
            LastMileRouteEvaluator.Evaluation evaluation
    ) { }
}
