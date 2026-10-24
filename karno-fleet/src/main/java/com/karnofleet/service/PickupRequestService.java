package com.karnofleet.service;

import com.karnofleet.entity.PickupRequest;
import com.karnofleet.dto.request.CreatePickupRequest;
import com.karnofleet.dto.request.UpdatePickupRequest;
import com.karnofleet.dto.response.PickupRequestResponse;

import java.util.List;
import java.util.UUID;

public interface PickupRequestService {
    List<PickupRequest> findPlanningCandidates(UUID hubId);
    PickupRequestResponse create(CreatePickupRequest request);
    PickupRequestResponse update(UUID id, UpdatePickupRequest request);
    void cancel(UUID id, UUID sellerId);
    List<PickupRequestResponse> findByShop(UUID shopId, UUID sellerId);
}
