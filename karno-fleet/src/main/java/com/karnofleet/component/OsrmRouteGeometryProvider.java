package com.karnofleet.component;

import com.karnofleet.config.OsrmProperties;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.response.OsrmRouteResponse;
import com.karnofleet.dto.response.RouteGeometryResponse;
import com.karnofleet.enums.PolylineFormat;
import com.karnofleet.exception.RoutingProviderException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "routing.geometry.provider",
        havingValue = "osrm",
        matchIfMissing = true
)
public class OsrmRouteGeometryProvider implements RouteGeometryProvider {
    private final WebClient osrmWebClient;
    private final OsrmProperties properties;

    @Override
    public RouteGeometryResponse compute(GeoPoint start, List<GeoPoint> orderedStops, GeoPoint hub, LocalDateTime departureAt) {
        List<GeoPoint> points = new ArrayList<>();

        points.add(start);
        points.addAll(orderedStops);
        points.add(hub);

        String coordinates = points.stream()
                        .map(this::toCoordinate)
                        .collect(Collectors.joining(";"));

        OsrmRouteResponse response = osrmWebClient
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder
                                        .path("/route/v1/" + properties.profile())
                                                .path("/" + coordinates)
                                                .queryParam("overview", "full")
                                                .queryParam("geometries", "polyline")
                                                .build())
                        .retrieve()
                        .bodyToMono(OsrmRouteResponse.class)
                        .block();

        if(response == null || !"Ok".equals(response.code()) || response.routes().isEmpty()){
            throw new RoutingProviderException("OSRM route failed");
        }

        OsrmRouteResponse.Route route = response.routes().getFirst();

        return new RouteGeometryResponse(
                route.geometry(),
                PolylineFormat.OSRM,
                (long)route.distance(),
                Duration.ofSeconds((long)route.duration())
        );
    }

    private String toCoordinate(GeoPoint point){
        return point.longitude() + "," + point.latitude();
    }
}
