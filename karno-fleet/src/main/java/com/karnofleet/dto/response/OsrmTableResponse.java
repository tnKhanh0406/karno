package com.karnofleet.dto.response;

import java.util.List;

public record OsrmTableResponse(
        String code,
        List<List<Double>> durations
) {
}
