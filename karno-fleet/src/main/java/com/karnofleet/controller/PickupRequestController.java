package com.karnofleet.controller;

import com.karnofleet.entity.PickupRequest;
import com.karnofleet.service.PickupRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pickup-requests")
@RequiredArgsConstructor
public class PickupRequestController {

    private final PickupRequestService pickupRequestService;

    @GetMapping
    public List<PickupRequest> getPickupRequests(@RequestParam UUID hubId) {
        return pickupRequestService.findPlanningCandidates(hubId);
    }
}
