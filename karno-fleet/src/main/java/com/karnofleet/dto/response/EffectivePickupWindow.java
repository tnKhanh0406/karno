package com.karnofleet.dto.response;

import java.time.LocalDateTime;

public record EffectivePickupWindow(
        LocalDateTime start,
        LocalDateTime end
) {
}
