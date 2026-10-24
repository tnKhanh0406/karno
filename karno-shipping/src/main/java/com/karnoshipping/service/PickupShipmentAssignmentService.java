package com.karnoshipping.service;

import com.karnoshipping.dto.request.PickupShipmentAssignmentRequest;
import com.karnoshipping.dto.response.PickupShipmentSnapshot;

import java.util.List;
import java.util.UUID;

public interface PickupShipmentAssignmentService {
    List<PickupShipmentSnapshot> claim(UUID pickupRequestId, PickupShipmentAssignmentRequest request);
    List<PickupShipmentSnapshot> replace(UUID pickupRequestId, PickupShipmentAssignmentRequest request);
    void release(UUID pickupRequestId, UUID shopId);
}
