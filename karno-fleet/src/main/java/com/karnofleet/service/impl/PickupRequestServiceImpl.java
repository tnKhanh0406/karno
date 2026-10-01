package com.karnofleet.service.impl;

import com.karnofleet.entity.PickupRequest;
import com.karnofleet.enums.PickupRequestStatus;
import com.karnofleet.repository.PickupRequestRepo;
import com.karnofleet.service.PickupRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PickupRequestServiceImpl implements PickupRequestService {
    private final PickupRequestRepo pickupRequestRepo;

    @Override
    public List<PickupRequest> findPlanningCandidates(UUID hubId) {
        return pickupRequestRepo.findPlanningCandidates(hubId, PickupRequestStatus.WAITING_PLANNING);
    }
}
