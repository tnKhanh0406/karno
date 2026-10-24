package com.karnoshipping.service;

import com.karnoshipping.dto.response.ReadyShipmentResponse;

import java.util.List;
import java.util.UUID;

public interface ShipmentQueryService {
    List<ReadyShipmentResponse> getReadyForPickup(UUID shopId);
}
