package com.karnofleet.component;

import com.karnofleet.config.OsrmProperties;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.request.RouteKey;
import com.karnofleet.dto.response.OsrmTableResponse;
import com.karnofleet.exception.RoutingProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OsrmMatrixLoader implements TravelTimeMatrixLoader{
    private final WebClient osrmWebClient;
    private final OsrmProperties properties;

    @Override
    public TravelTimeProvider load(List<GeoPoint> points, LocalDateTime plannedAt) {
        String coordinates = points.stream()
                        .map(this::toCoordinate)
                        .collect(Collectors.joining(";"));

        OsrmTableResponse response = osrmWebClient
                        .get()
                        .uri(uriBuilder ->
                            uriBuilder
                                .path("/table/v1/" + properties.profile())
                                    .path("/" + coordinates)
                                    .queryParam("annotations", "duration")
                                    .build())
                        .retrieve()
                        .bodyToMono(OsrmTableResponse.class)
                        .block();
        if (response == null || !"Ok".equals(response.code())) {
            throw new RoutingProviderException("OSRM table failed");
        }
        log.info(
                "OSRM matrix loaded: {} points",
                points.size()
        );
        log.info(
                "OSRM response code={}",
                response.code()
        );
        return buildProvider(
                points,
                response
        );
    }

    private String toCoordinate(GeoPoint point) {
        return point.longitude() + "," + point.latitude();
    }

    private TravelTimeProvider buildProvider(
            List<GeoPoint> points,
            OsrmTableResponse response
    ) {
        Map<RouteKey, Duration> matrix = new HashMap<>();

        List<List<Double>> durations = response.durations();

        for (int from = 0; from < points.size(); from++) {
            for (int to = 0; to < points.size(); to++) {

                Double seconds = durations.get(from).get(to);

                if(seconds == null){
                    continue;
                }

                matrix.put(
                        RouteKey.of(
                                points.get(from),
                                points.get(to)
                        ),
                        Duration.ofSeconds(seconds.longValue())
                );
            }
        }

        return new MatrixTravelTimeProvider(matrix);
    }
}
