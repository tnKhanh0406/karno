package com.karnofleet.component;

import com.karnofleet.dto.response.EffectivePickupWindow;
import com.karnofleet.dto.request.PickupStopInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PickupPriorityResolver {

    private final EffectivePickupWindowResolver windowResolver;

    public List<PickupStopInput> sort(List<PickupStopInput> requests,
                                      LocalDateTime plannedAt) {
        Comparator<PickupStopInput> comparator = Comparator
                .comparing((PickupStopInput request) -> effectiveEnd(request, plannedAt))
                .thenComparing(PickupStopInput::createdAt)
                .thenComparing(PickupStopInput::requestId);

        return requests.stream()
                .sorted(comparator)
                .toList();
    }

    private LocalDateTime effectiveEnd(PickupStopInput request,
                                       LocalDateTime plannedAt) {
        EffectivePickupWindow window = windowResolver.resolve(
                        plannedAt,
                        request.windowStart(),
                        request.windowEnd()
        );
        return window.end();
    }
}
