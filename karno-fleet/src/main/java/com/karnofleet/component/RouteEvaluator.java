package com.karnofleet.component;

import com.karnofleet.dto.response.EffectivePickupWindow;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.response.EvaluatedStop;
import com.karnofleet.dto.response.RouteEvaluationResult;
import com.karnofleet.enums.RouteViolation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.karnofleet.dto.request.RouteEvaluationRequest;
import com.karnofleet.dto.request.PickupStopInput;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RouteEvaluator {
    private final EffectivePickupWindowResolver windowResolver;

    public RouteEvaluationResult evaluate(RouteEvaluationRequest request, TravelTimeProvider travelTimeProvider) {
        List<EvaluatedStop> evaluatedStops = new ArrayList<>();
        List<RouteViolation> violations = new ArrayList<>();

        GeoPoint currentLocation = request.startLocation();

        LocalDateTime currentDeparture = request.plannedStartAt();

        BigDecimal currentWeight = request.initialWeight();
        BigDecimal currentVolume = request.initialVolume();

        int sequence = 1;

        for (PickupStopInput stop : request.stops()) {
            Duration travelTime;
            try {
                travelTime = travelTimeProvider.getTravelTime(
                        currentLocation,
                        stop.location(),
                        currentDeparture
                );
            } catch (Exception ex) {
                violations.add(RouteViolation.TRAVEL_TIME_UNAVAILABLE);
                return buildFailedResult(
                        evaluatedStops,
                        currentWeight,
                        currentVolume,
                        violations
                );
            }

            //1. Tính thời gian tới shop
            LocalDateTime arrivalAt = currentDeparture.plus(travelTime);

            //2. Tính window theo ngày hiện tại
            EffectivePickupWindow window = windowResolver.resolve(
                            arrivalAt,
                            stop.windowStart(),
                            stop.windowEnd()
            );

            //3. Nếu tới trước window thì phải chờ
            LocalDateTime serviceStartAt = arrivalAt.isBefore(window.start())
                            ? window.start()
                            : arrivalAt;

            //4. Thời gian hoàn thành dịch vụ tại shop
            LocalDateTime departureAt = serviceStartAt.plusSeconds(stop.serviceDurationSeconds());

            //5. Kiểm tra vi phạm window
            if (departureAt.isAfter(window.end())) {
                violations.add(RouteViolation.PICKUP_WINDOW_VIOLATION);
            }

            //6. Tính tải trọng và thể tích sau khi dừng tại shop
            BigDecimal loadWeightAfter = currentWeight.add(stop.weight());
            BigDecimal loadVolumeAfter = currentVolume.add(stop.volume());

            //7. Kiểm tra vi phạm tải trọng và thể tích
            if (loadWeightAfter.compareTo(request.capacityWeight()) > 0) {
                violations.add(RouteViolation.CAPACITY_WEIGHT_EXCEEDED);
            }

            if (loadVolumeAfter.compareTo(request.capacityVolume()) > 0) {
                violations.add(RouteViolation.CAPACITY_VOLUME_EXCEEDED);
            }

            //8. Tính thời gian chờ
            Duration waiting = Duration.between(arrivalAt, serviceStartAt);

            //9. Lưu thông tin dừng tại shop
            EvaluatedStop evaluatedStop =
                    EvaluatedStop.builder()
                            .sequence(sequence++)
                            .requestId(stop.requestId())
                            .requestCode(stop.requestCode())
                            .location(stop.location())
                            .arrivalAt(arrivalAt)
                            .windowStartAt(window.start())
                            .windowEndAt(window.end())
                            .serviceStartAt(serviceStartAt)
                            .departureAt(departureAt)
                            .waitingTime(waiting)
                            .loadWeightAfter(loadWeightAfter)
                            .loadVolumeAfter(loadVolumeAfter)
                            .build();

            evaluatedStops.add(evaluatedStop);

            //10. Cập nhật trạng thái hiện tại cho lần lặp tiếp theo
            currentLocation = stop.location();
            currentDeparture = departureAt;
            currentWeight = loadWeightAfter;
            currentVolume = loadVolumeAfter;
        }

        // Tính thời gian quay về hub
        Duration returnTravel;

        try {
            returnTravel = travelTimeProvider.getTravelTime(
                            currentLocation,
                            request.hubLocation(),
                            currentDeparture
            );

        } catch (Exception ex) {
            violations.add(RouteViolation.TRAVEL_TIME_UNAVAILABLE);
            return buildFailedResult(
                    evaluatedStops,
                    currentWeight,
                    currentVolume,
                    violations
            );
        }

        LocalDateTime returnToHubAt = currentDeparture.plus(returnTravel);

        // Kiểm tra vi phạm thời gian kết thúc ca
        if (returnToHubAt.isAfter(request.shiftEndAt())) {
            violations.add(RouteViolation.SHIFT_END_EXCEEDED);
        }

        return RouteEvaluationResult.builder()
                .feasible(violations.isEmpty())
                .stops(evaluatedStops)
                .returnToHubAt(returnToHubAt)
                .finalWeight(currentWeight)
                .finalVolume(currentVolume)
                .violations(violations)
                .build();
    }


    private RouteEvaluationResult buildFailedResult(
            List<EvaluatedStop> evaluatedStops,
            BigDecimal currentWeight,
            BigDecimal currentVolume,
            List<RouteViolation> violations
    ) {

        return RouteEvaluationResult.builder()
                .feasible(false)
                .stops(evaluatedStops)
                .returnToHubAt(null)
                .finalWeight(currentWeight)
                .finalVolume(currentVolume)
                .violations(violations)
                .build();
    }
}
