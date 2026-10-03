package com.karnofleet.component;

import com.karnofleet.config.HereMatrixProperties;
import com.karnofleet.dto.request.*;
import com.karnofleet.dto.response.HereMatrixResponse;
import com.karnofleet.exception.RoutingProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "routing.matrix.provider",
        havingValue = "here",
        matchIfMissing = true
)
public class HereMatrixLoader implements TravelTimeMatrixLoader {

    private final WebClient hereMatrixWebClient;
    private final HereMatrixProperties properties;

    @Override
    public TravelTimeProvider load(
            List<GeoPoint> points,
            LocalDateTime plannedAt
    ) {

        if (points.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot load HERE matrix with no points"
            );
        }

        List<HereLocation> locations =
                points.stream()
                        .map(this::toHereLocation)
                        .toList();

        String departureTime =
                plannedAt
                        .atZone(
                                ZoneId.of(properties.getZoneId())
                        )
                        .toOffsetDateTime()
                        .toString();

        HereMatrixRequest request =
                new HereMatrixRequest(
                        locations,
                        locations,
                        new HereRegionDefinition(
                                "autoCircle",
                                properties.getRegionMarginMeters()
                        ),
                        departureTime,
                        properties.getRoutingMode(),
                        properties.getTransportMode(),
                        new HereVehicle(
                                properties.getVehicle().getCurrentWeightKg(),
                                properties.getVehicle().getGrossWeightKg()
                        ),
                        List.of(
                                "travelTimes",
                                "distances"
                        )
                );

        log.info(
                "Loading HERE truck matrix: points={}, departureTime={}",
                points.size(),
                departureTime
        );

        HereMatrixResponse response =
                hereMatrixWebClient
                        .post()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .path("/v8/matrix")
                                        .queryParam("async", false)
                                        .queryParam(
                                                "apiKey",
                                                properties.getApiKey()
                                        )
                                        .build()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(HereMatrixResponse.class)
                        .block();

        if (response == null
                || response.matrix() == null) {

            throw new RoutingProviderException(
                    "HERE Matrix returned no matrix"
            );
        }

        return buildProvider(points, response.matrix());
    }

    private HereLocation toHereLocation(
            GeoPoint point
    ) {
        return new HereLocation(
                point.latitude(),
                point.longitude()
        );
    }

    private TravelTimeProvider buildProvider(
            List<GeoPoint> points,
            HereMatrixResponse.Matrix matrix
    ) {

        int numOrigins = matrix.numOrigins();
        int numDestinations = matrix.numDestinations();

        List<Long> travelTimes = matrix.travelTimes();
        List<Integer> distances = matrix.distances();
        List<Integer> errorCodes = matrix.errorCodes();

        Map<RouteKey, Duration> durations =
                new HashMap<>();

        for (int i = 0; i < numOrigins; i++) {

            for (int j = 0; j < numDestinations; j++) {

                int index =
                        numDestinations * i + j;

                if (errorCodes != null
                        && !errorCodes.isEmpty()
                        && errorCodes.get(index) != 0) {

                    log.warn(
                            "HERE matrix failed: from={} to={}, errorCode={}",
                            i,
                            j,
                            errorCodes.get(index)
                    );

                    continue;
                }

                long seconds =
                        travelTimes.get(index);

//                int meters =
//                        distances.get(index);

//                double averageSpeedKmh =
//                        seconds > 0
//                                ? meters / 1000.0
//                                / (seconds / 3600.0)
//                                : 0;

//                log.info(
//                        "HERE {} -> {} | distance={} m | time={} sec | avgSpeed={} km/h",
//                        i,
//                        j,
//                        meters,
//                        seconds,
//                        String.format("%.1f", averageSpeedKmh)
//                );

                durations.put(
                        RouteKey.of(
                                points.get(i),
                                points.get(j)
                        ),
                        Duration.ofSeconds(seconds)
                );
            }
        }

        return new MatrixTravelTimeProvider(durations);
    }
}
