package com.karnofleet.dto.request;

import java.math.BigDecimal;

public record RouteKey(
        String fromLat,
        String fromLng,
        String toLat,
        String toLng
) {

    public static RouteKey of(
            GeoPoint from,
            GeoPoint to
    ) {
        return new RouteKey(
                normalize(from.latitude()),
                normalize(from.longitude()),
                normalize(to.latitude()),
                normalize(to.longitude())
        );
    }

    private static String normalize(BigDecimal value) {
        return value
                .stripTrailingZeros()
                .toPlainString();
    }
}
