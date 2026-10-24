package com.karnofleet.dto.response;

import com.karnofleet.enums.PickupRequestStatus;
import java.util.List;
import java.util.UUID;

public record PickupConfirmationResponse(
        UUID pickupRequestId,
        PickupRequestStatus status,
        List<UUID> shipmentIds,
        boolean alreadyPickedUp
) {}
