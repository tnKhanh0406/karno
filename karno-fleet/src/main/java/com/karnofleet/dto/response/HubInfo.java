package com.karnofleet.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record HubInfo(
        UUID id,
        String hubCode,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String status
) {
}
