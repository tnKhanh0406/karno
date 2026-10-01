package com.karnofleet;

import com.karnofleet.component.*;
import com.karnofleet.dto.request.*;
import com.karnofleet.dto.response.PlanningResult;
import com.karnofleet.dto.response.UnassignedRequestResponse;
import com.karnofleet.enums.UnassignedReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class CheapestFeasibleInsertionPlannerTest {

    private static final GeoPoint START = point("21.0285", "105.8542");
    private static final GeoPoint A = point("21.0185", "105.8290");
    private static final GeoPoint B = point("21.0335", "105.8140");
    private static final GeoPoint C = point("21.0365", "105.7905");
    private static final GeoPoint HUB = point("21.0500", "105.8500");

    private CheapestFeasibleInsertionPlanner createPlanner(TravelTimeProvider provider) {
        EffectivePickupWindowResolver windowResolver = new EffectivePickupWindowResolver();
        RouteEvaluator evaluator = new RouteEvaluator(windowResolver);
        PickupPriorityResolver priorityResolver = new PickupPriorityResolver(windowResolver);
        return new CheapestFeasibleInsertionPlanner(
                evaluator,
                priorityResolver
        );
    }

    public static GeoPoint point(String lat, String lng) {
        return new GeoPoint(
                new BigDecimal(lat),
                new BigDecimal(lng)
        );
    }

    private PickupStopInput stop(
            String code,
            GeoPoint location,
            String weight,
            String volume,
            LocalTime start,
            LocalTime end,
            LocalDateTime createdAt
    ) {

        return PickupStopInput.builder()
                .requestId(UUID.randomUUID())
                .requestCode(code)
                .location(location)
                .weight(new BigDecimal(weight))
                .volume(new BigDecimal(volume))
                .windowStart(start)
                .windowEnd(end)
                .serviceDurationSeconds(300)
                .createdAt(createdAt)
                .build();
    }

    private PlanningRequest planningRequest(
            LocalDateTime plannedAt,
            PickupStopInput... requests
    ) {
        return PlanningRequest.builder()
                .startLocation(START)
                .plannedStartAt(plannedAt)
                .hubLocation(HUB)
                .shiftEndAt(plannedAt.toLocalDate().atTime(17, 0))
                .capacityWeight(new BigDecimal("100"))
                .capacityVolume(new BigDecimal("1.00"))
                .initialWeight(BigDecimal.ZERO)
                .initialVolume(BigDecimal.ZERO)
                .pickupRequests(Arrays.asList(requests))
                .build();
    }

    @Test
    void shouldInsertSingleRequest() {
        TravelTimeProvider provider =
                new TestTravelMatrix()
                        .add(START, A, 10)
                        .add(A, HUB, 10)
                        .build();

        CheapestFeasibleInsertionPlanner planner = createPlanner(provider);

        PickupStopInput a =
                stop(
                        "A",
                        A,
                        "20",
                        "0.10",
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),
                        LocalDateTime.of(
                                2026, 9, 29,
                                7, 0
                        )
                );

        PlanningResult result =
                planner.plan(
                        planningRequest(
                                LocalDateTime.of(
                                        2026, 9, 29,
                                        8, 0
                                ),
                                a
                        ),
                        provider
                );

        assertThat(result.selectedStops())
                .extracting(PickupStopInput::requestCode)
                .containsExactly("A");

        assertThat(result.unassigned()).isEmpty();
        assertThat(result.routeEvaluation().feasible()).isTrue();
    }

    @Test
    void shouldChooseInsertionWithEarlierReturnToHub() {
        TravelTimeProvider provider =
                new TestTravelMatrix()
                        .add(START, A, 10)
                        .add(A, HUB, 10)
                        .add(START, B, 5)
                        .add(B, A, 30)
                        .add(A, B, 5)
                        .add(B, HUB, 5)
                        .build();

        CheapestFeasibleInsertionPlanner planner = createPlanner(provider);

        LocalDateTime created =
                LocalDateTime.of(
                        2026, 9, 29,
                        7, 0
                );

        PickupStopInput a =
                stop(
                        "A",
                        A,
                        "20",
                        "0.10",
                        LocalTime.of(8, 0),
                        LocalTime.of(10, 0),
                        created
                );

        PickupStopInput b =
                stop(
                        "B",
                        B,
                        "20",
                        "0.10",
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),
                        created.plusMinutes(1)
                );

        PlanningResult result =
                planner.plan(
                        planningRequest(
                                LocalDateTime.of(
                                        2026, 9, 29,
                                        8, 0
                                ),
                                a,
                                b
                        ),
                        provider
                );

        assertThat(result.selectedStops())
                .extracting(PickupStopInput::requestCode)
                .containsExactly("A", "B");
    }

    @Test
    void shouldRejectCheaperCandidateWhenItViolatesWindow() {
        TravelTimeProvider provider = new TestTravelMatrix()
                        // route A ban đầu
                        .add(START, A, 10)
                        .add(A, HUB, 10)
                        // B -> A nhanh nhưng làm A trễ
                        .add(START, B, 1)
                        .add(B, A, 10)
                        // A -> B feasible
                        .add(A, B, 1)
                        .add(B, HUB, 10)
                        .build();

        CheapestFeasibleInsertionPlanner planner = createPlanner(provider);

        LocalDateTime created =
                LocalDateTime.of(
                        2026, 9, 29,
                        7, 0
                );

        PickupStopInput a =
                stop(
                        "A",
                        A,
                        "20",
                        "0.10",
                        LocalTime.of(8, 0),
                        LocalTime.of(8, 20),
                        created
                );

        PickupStopInput b =
                stop(
                        "B",
                        B,
                        "20",
                        "0.10",
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),
                        created.plusMinutes(1)
                );

        PlanningResult result =
                planner.plan(
                        planningRequest(
                                LocalDateTime.of(
                                        2026, 9, 29,
                                        8, 0
                                ),
                                a,
                                b
                        ),
                        provider
                );

        assertThat(result.selectedStops())
                .extracting(PickupStopInput::requestCode)
                .containsExactly("A", "B");
        assertThat(result.routeEvaluation().feasible()).isTrue();
    }

    @Test
    void shouldUnassignRequestThatAloneExceedsCapacity() {
        TravelTimeProvider provider = new TestTravelMatrix().build();
        CheapestFeasibleInsertionPlanner planner = createPlanner(provider);

        PickupStopInput a =
                stop(
                        "A",
                        A,
                        "120",
                        "0.10",
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),
                        LocalDateTime.of(
                                2026, 9, 29,
                                7, 0
                        )
                );

        PlanningResult result =
                planner.plan(
                        planningRequest(
                                LocalDateTime.of(
                                        2026, 9, 29,
                                        8, 0
                                ),
                                a
                        ),
                        provider
                );

        assertThat(result.selectedStops()).isEmpty();
        assertThat(result.unassigned()).hasSize(1);
        assertThat(
                result.unassigned()
                        .get(0)
                        .reason())
                .isEqualTo(UnassignedReason.REQUEST_EXCEEDS_CAPACITY);
    }

    @Test
    void shouldUnassignRequestWhenCombinedLoadExceedsCapacity() {

        TravelTimeProvider provider =
                new TestTravelMatrix()

                        .add(START, A, 10)
                        .add(A, HUB, 10)

                        .add(START, B, 10)
                        .add(B, A, 10)

                        .add(A, B, 10)
                        .add(B, HUB, 10)

                        .build();


        CheapestFeasibleInsertionPlanner planner =
                createPlanner(provider);


        LocalDateTime created =
                LocalDateTime.of(
                        2026, 9, 29,
                        7, 0
                );


        PickupStopInput a =
                stop(
                        "A",
                        A,
                        "60",
                        "0.20",
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),
                        created
                );


        PickupStopInput b =
                stop(
                        "B",
                        B,
                        "50",
                        "0.20",
                        LocalTime.of(8, 0),
                        LocalTime.of(13, 0),
                        created.plusMinutes(1)
                );


        PlanningResult result =
                planner.plan(
                        planningRequest(
                                LocalDateTime.of(
                                        2026, 9, 29,
                                        8, 0
                                ),
                                a,
                                b
                        ),
                        provider
                );


        assertThat(result.selectedStops())
                .extracting(
                        PickupStopInput::requestCode
                )
                .containsExactly("A");


        assertThat(result.unassigned())
                .hasSize(1);


        assertThat(
                result.unassigned()
                        .get(0)
                        .requestCode()
        )
                .isEqualTo("B");


        assertThat(
                result.unassigned()
                        .get(0)
                        .reason()
        )
                .isEqualTo(
                        UnassignedReason.NO_FEASIBLE_INSERTION
                );
    }

    @Test
    void shouldContinuePlanningAfterOneRequestFails() {
        TravelTimeProvider provider = new TestTravelMatrix()
                        .add(START, A, 10)
                        .add(A, HUB, 10)
                        .add(START, C, 20)
                        .add(C, A, 20)
                        .add(A, C, 5)
                        .add(C, HUB, 5)
                        .build();

        CheapestFeasibleInsertionPlanner planner =
                createPlanner(provider);


        LocalDateTime created =
                LocalDateTime.of(
                        2026, 9, 29,
                        7, 0
                );


        PickupStopInput a =
                stop(
                        "A",
                        A,
                        "40",
                        "0.10",
                        LocalTime.of(8, 0),
                        LocalTime.of(10, 0),
                        created
                );


        PickupStopInput b =
                stop(
                        "B",
                        B,
                        "120",
                        "0.10",
                        LocalTime.of(8, 0),
                        LocalTime.of(11, 0),
                        created.plusMinutes(1)
                );


        PickupStopInput c =
                stop(
                        "C",
                        C,
                        "20",
                        "0.10",
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),
                        created.plusMinutes(2)
                );


        PlanningResult result =
                planner.plan(
                        planningRequest(
                                LocalDateTime.of(
                                        2026, 9, 29,
                                        8, 0
                                ),
                                a,
                                b,
                                c
                        ),
                        provider
                );


        assertThat(result.selectedStops())
                .extracting(
                        PickupStopInput::requestCode
                )
                .containsExactly(
                        "A",
                        "C"
                );


        assertThat(result.unassigned())
                .extracting(
                        UnassignedRequestResponse::requestCode
                )
                .containsExactly("B");
    }

    @Test
    void shouldPrioritizeTodayWindowBeforeTomorrowWindow() {
        EffectivePickupWindowResolver windowResolver = new EffectivePickupWindowResolver();

        PickupPriorityResolver resolver = new PickupPriorityResolver(windowResolver);

        LocalDateTime created =
                LocalDateTime.of(
                        2026, 9, 29,
                        8, 0
                );

        PickupStopInput a =
                stop(
                        "A",
                        A,
                        "10",
                        "0.10",
                        LocalTime.of(8, 0),
                        LocalTime.of(10, 0),
                        created
                );

        PickupStopInput b =
                stop(
                        "B",
                        B,
                        "10",
                        "0.10",
                        LocalTime.of(16, 0),
                        LocalTime.of(18, 0),
                        created.plusMinutes(1)
                );

        List<PickupStopInput> sorted =
                resolver.sort(
                        List.of(a, b),
                        LocalDateTime.of(
                                2026, 9, 29,
                                16, 0
                        )
                );

        assertThat(sorted)
                .extracting(
                        PickupStopInput::requestCode
                )
                .containsExactly(
                        "B",
                        "A"
                );
    }

    @Test
    void shouldUseCreatedAtWhenEffectiveDeadlineIsEqual() {
        EffectivePickupWindowResolver windowResolver = new EffectivePickupWindowResolver();

        PickupPriorityResolver resolver = new PickupPriorityResolver(windowResolver);

        PickupStopInput a =
                stop(
                        "A",
                        A,
                        "10",
                        "0.10",
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),

                        LocalDateTime.of(
                                2026, 9, 29,
                                7, 0
                        )
                );

        PickupStopInput b =
                stop(
                        "B",
                        B,
                        "10",
                        "0.10",
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),

                        LocalDateTime.of(
                                2026, 9, 29,
                                7, 30
                        )
                );

        List<PickupStopInput> sorted =
                resolver.sort(
                        List.of(b, a),
                        LocalDateTime.of(
                                2026, 9, 29,
                                8, 0
                        )
                );

        assertThat(sorted)
                .extracting(
                        PickupStopInput::requestCode
                )
                .containsExactly(
                        "A",
                        "B"
                );
    }
}

class TestTravelMatrix {

    private final Map<RouteKey, Duration> matrix = new HashMap<>();

    TestTravelMatrix add(
            GeoPoint from,
            GeoPoint to,
            long minutes
    ) {

        matrix.put(
                RouteKey.of(from, to),
                Duration.ofMinutes(minutes)
        );

        return this;
    }

    MatrixTravelTimeProvider build() {
        return new MatrixTravelTimeProvider(matrix);
    }
}
