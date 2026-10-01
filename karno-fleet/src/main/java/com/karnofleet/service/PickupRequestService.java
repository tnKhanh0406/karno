package com.karnofleet.service;

import com.karnofleet.entity.PickupRequest;

import java.util.List;
import java.util.UUID;

public interface PickupRequestService {
    List<PickupRequest> findPlanningCandidates(UUID hubId);
}
