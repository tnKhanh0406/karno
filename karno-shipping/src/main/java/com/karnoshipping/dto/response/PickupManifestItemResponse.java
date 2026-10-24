package com.karnoshipping.dto.response;

import com.karnoshipping.entity.Shipment;
import com.karnoshipping.enums.ShipmentStatus;
import java.util.UUID;

public record PickupManifestItemResponse(
        UUID shipmentId, UUID orderId, String trackingCode,
        ShipmentStatus status, Integer weightGram
) {
    public static PickupManifestItemResponse from(Shipment shipment) {
        return new PickupManifestItemResponse(shipment.getId(), shipment.getOrderId(),
                shipment.getTrackingCode(), shipment.getStatus(), shipment.getTotalWeightGram());
    }
}
