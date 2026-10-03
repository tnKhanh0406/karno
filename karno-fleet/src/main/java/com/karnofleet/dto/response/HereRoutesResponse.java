package com.karnofleet.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HereRoutesResponse(
        List<HereRoute> routes
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HereRoute(
            List<HereRouteSection> sections
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HereRouteSection(
            String polyline,
            HereRouteSummary summary
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HereRouteSummary(
            long duration,
            long length
    ) {
    }
}