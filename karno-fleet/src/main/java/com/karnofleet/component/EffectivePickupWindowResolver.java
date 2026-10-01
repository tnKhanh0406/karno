package com.karnofleet.component;

import com.karnofleet.dto.response.EffectivePickupWindow;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class EffectivePickupWindowResolver {

    public EffectivePickupWindow resolve(LocalDateTime arrivalAt, LocalTime windowStart, LocalTime windowEnd) {
        if (!windowStart.isBefore(windowEnd)) {
            throw new IllegalArgumentException("Overnight pickup window is not supported");
        }

        LocalDate date = arrivalAt.toLocalDate();
        LocalDateTime startToday = LocalDateTime.of(date, windowStart);
        LocalDateTime endToday = LocalDateTime.of(date, windowEnd);

        if (!arrivalAt.isAfter(endToday)) {
            return new EffectivePickupWindow(startToday, endToday);
        }

        LocalDate tomorrow = date.plusDays(1);
        return new EffectivePickupWindow(LocalDateTime.of(tomorrow, windowStart), LocalDateTime.of(tomorrow, windowEnd));
    }
}
