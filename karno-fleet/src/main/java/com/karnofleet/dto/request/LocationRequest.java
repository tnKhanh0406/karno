package com.karnofleet.dto.request;

import java.math.BigDecimal;

public record LocationRequest(
        BigDecimal latitude,
        BigDecimal longitude
) {
}