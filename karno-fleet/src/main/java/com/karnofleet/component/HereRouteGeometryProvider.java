package com.karnofleet.component;

import com.karnofleet.config.HereRoutingProperties;
import com.karnofleet.dto.request.GeoPoint;
import com.karnofleet.dto.response.HereRoutesResponse;
import com.karnofleet.dto.response.RouteGeometryResponse;
import com.karnofleet.enums.PolylineFormat;
import com.karnofleet.exception.RoutingProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(
        name = "routing.geometry.provider",
        havingValue = "here"
)
public class HereRouteGeometryProvider
        implements RouteGeometryProvider {
    private static final DateTimeFormatter HERE_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final WebClient hereRoutesWebClient;
    private final HereRoutingProperties properties;

    public HereRouteGeometryProvider(
            WebClient hereRoutesWebClient,
            HereRoutingProperties properties
    ) {
        this.hereRoutesWebClient = hereRoutesWebClient;
        this.properties = properties;
    }

    @Override
    public RouteGeometryResponse compute(
            GeoPoint start,
            List<GeoPoint> orderedStops,
            GeoPoint hub,
            LocalDateTime departureAt
    ) {

        String departureTime =
                departureAt
                        .atZone(
                                ZoneId.of(
                                        properties.getZoneId()
                                )
                        )
                        .format(HERE_DATE_TIME_FORMAT);

        HereRoutesResponse response;

        try {

            response = hereRoutesWebClient
                    .get()
                    .uri(uriBuilder ->
                            buildRouteUri(
                                    uriBuilder,
                                    start,
                                    orderedStops,
                                    hub,
                                    departureTime
                            )
                    )
                    .retrieve()

                    .onStatus(
                            status -> status.isError(),
                            responseSpec ->
                                    responseSpec
                                            .bodyToMono(String.class)
                                            .defaultIfEmpty("")
                                            .map(body ->
                                                    new RoutingProviderException(
                                                            "HERE Routes HTTP "
                                                                    + responseSpec.statusCode()
                                                                    + ": "
                                                                    + body
                                                    )
                                            )
                    )

                    .bodyToMono(
                            HereRoutesResponse.class
                    )

                    .timeout(
                            properties.getTimeout()
                    )

                    .onErrorMap(
                            ex ->
                                    !(ex
                                            instanceof RoutingProviderException),
                            ex ->
                                    new RoutingProviderException(
                                            "HERE Routes request failed: "
                                                    + ex.getMessage()
                                    )
                    )

                    .block();

        } catch (RoutingProviderException ex) {
            throw ex;

        } catch (Exception ex) {
            throw new RoutingProviderException(
                    "HERE Routes calculation failed: "
                            + ex.getMessage()
            );
        }

        if (
                response == null
                        || response.routes() == null
                        || response.routes().isEmpty()
        ) {
            throw new RoutingProviderException(
                    "HERE Routes returned no route"
            );
        }

        HereRoutesResponse.HereRoute route =
                response.routes().getFirst();

        if (
                route.sections() == null
                        || route.sections().isEmpty()
        ) {
            throw new RoutingProviderException(
                    "HERE Routes returned no sections"
            );
        }

        /*
         * Vì các via đều là passThrough=true,
         * HERE thường trả về một section duy nhất.
         */
        if (route.sections().size() != 1) {
            throw new RoutingProviderException(
                    "HERE Routes returned "
                            + route.sections().size()
                            + " sections; "
                            + "multi-section geometry is not supported yet"
            );
        }

        HereRoutesResponse.HereRouteSection section =
                route.sections().getFirst();

        if (
                section.polyline() == null
                        || section.polyline().isBlank()
        ) {
            throw new RoutingProviderException(
                    "HERE Routes returned empty polyline"
            );
        }

        if (section.summary() == null) {
            throw new RoutingProviderException(
                    "HERE Routes returned no summary"
            );
        }

        return new RouteGeometryResponse(
                section.polyline(),
                PolylineFormat.HERE_FLEXIBLE,
                section.summary().length(),
                Duration.ofSeconds(
                        section.summary().duration()
                )
        );
    }

    private URI buildRouteUri(
            UriBuilder uriBuilder,
            GeoPoint start,
            List<GeoPoint> orderedStops,
            GeoPoint hub,
            String departureTime
    ) {

        uriBuilder
                .path("/v8/routes")

                .queryParam(
                        "transportMode",
                        properties.getTransportMode()
                )

                .queryParam(
                        "routingMode",
                        properties.getRoutingMode()
                )

                .queryParam(
                        "origin",
                        formatPoint(start)
                )

                .queryParam(
                        "destination",
                        formatPoint(hub)
                )

                .queryParam(
                        "departureTime",
                        departureTime
                )

                /*
                 * Thêm passthrough để sau này có thể
                 * kiểm tra HERE snap pickup vào đường nào.
                 */
                .queryParam(
                        "return",
                        "polyline,summary,passthrough"
                )

                .queryParam(
                        "apiKey",
                        properties.getApiKey()
                );

        /*
         * Các pickup phải được đi qua theo đúng
         * thứ tự planner đã chọn.
         */
        for (GeoPoint stop : orderedStops) {

            uriBuilder.queryParam(
                    "via",
                    formatPoint(stop)
                            + "!passThrough=true"
            );
        }

        /*
         * Scooter:
         * chỉ thêm option khi transportMode = scooter.
         */
        if (
                "scooter".equalsIgnoreCase(
                        properties.getTransportMode()
                )
        ) {

            uriBuilder.queryParam(
                    "scooter[allowHighway]",
                    properties.isScooterAllowHighway()
            );
        }

        /*
         * Truck:
         * thêm thông số trọng lượng xe.
         */
        if (
                "truck".equalsIgnoreCase(
                        properties.getTransportMode()
                )
        ) {

            if (
                    properties.getVehicleCurrentWeightKg()
                            != null
            ) {

                uriBuilder.queryParam(
                        "vehicle[currentWeight]",
                        properties
                                .getVehicleCurrentWeightKg()
                );
            }

            if (
                    properties.getVehicleGrossWeightKg()
                            != null
            ) {

                uriBuilder.queryParam(
                        "vehicle[grossWeight]",
                        properties
                                .getVehicleGrossWeightKg()
                );
            }
        }

        /*
         * Spring đã build URI và encode query param.
         */
        URI encodedUri = uriBuilder.build();

        /*
         * HERE yêu cầu:
         *
         * !passThrough=true
         *
         * nhưng Spring có thể tạo:
         *
         * !passThrough%3Dtrue
         *
         * Chỉ sửa đúng phần này.
         */
        String rawUri =
                encodedUri
                        .toString()
                        .replace(
                                "!passThrough%3Dtrue",
                                "!passThrough=true"
                        );

        /*
         * Log để kiểm tra request.
         * API key được mask.
         */
        String debugUri =
                rawUri.replace(
                        properties.getApiKey(),
                        "***"
                );

        log.debug(
                "HERE Routes URI: {}",
                debugUri
        );

        return URI.create(rawUri);
    }

    private String formatPoint(
            GeoPoint point
    ) {
        return point.latitude()
                + ","
                + point.longitude();
    }
}