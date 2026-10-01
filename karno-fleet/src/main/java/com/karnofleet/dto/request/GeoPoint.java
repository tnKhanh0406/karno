package com.karnofleet.dto.request;

import java.math.BigDecimal;

public record GeoPoint(
        BigDecimal latitude,
        BigDecimal longitude
) {
}
