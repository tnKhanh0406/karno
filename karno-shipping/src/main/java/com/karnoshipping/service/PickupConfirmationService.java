package com.karnoshipping.service;

import java.util.List;
import java.util.UUID;

public interface PickupConfirmationService {
    void confirm(UUID pickupRequestId, List<UUID> shipmentIds);
    void revert(UUID pickupRequestId, List<UUID> shipmentIds);
    void completeSession(List<UUID> pickedUpPickupRequestIds,
                         List<UUID> unpickedPickupRequestIds);
}
