package com.karnoshipping.messaging.payload;

import java.util.List;
import java.util.UUID;

public record PickupRequestAssignmentPayload(
        UUID pickupRequestId,
        UUID shopId,
        List<UUID> shipmentIds
) {
}
