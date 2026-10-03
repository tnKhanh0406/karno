package com.karnofleet.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record HereLocation(
        @JsonProperty("lat")
        BigDecimal lat,

        @JsonProperty("lng")
        BigDecimal lng
) {
}
