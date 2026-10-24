package com.karnofleet.client.dto;

import java.util.List;
import java.util.UUID;

public record PickupShipmentAssignmentRequest(UUID shopId, List<UUID> shipmentIds) {
}
