package com.karnofleet.service;

import com.karnofleet.dto.response.PickupConfirmationResponse;
import com.karnofleet.dto.response.PickupManifestResponse;
import java.util.UUID;

public interface PickupConfirmationService {
    PickupConfirmationResponse confirm(UUID pickupRequestId, UUID shipperId);
    PickupManifestResponse manifest(UUID pickupRequestId, UUID shipperId);
}
