package com.karnofleet.controller;

import com.karnofleet.dto.request.FirstMilePlanRequest;
import com.karnofleet.dto.response.FirstMilePreviewResponse;
import com.karnofleet.dto.response.PickupSessionResponse;
import com.karnofleet.dto.response.PickupConfirmationResponse;
import com.karnofleet.dto.response.PickupManifestResponse;
import com.karnofleet.entity.Vehicle;
import com.karnofleet.service.PickupSessionService;
import com.karnofleet.service.PickupConfirmationService;
import jakarta.validation.Valid;
import java.util.List;
import com.karnofleet.service.FirstMilePlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/first-mile")
@RequiredArgsConstructor
public class FirstMilePlanningController {

    private final FirstMilePlanningService planningService;
    private final PickupSessionService sessionService;
    private final PickupConfirmationService confirmationService;

    @PostMapping("/preview")
    public FirstMilePreviewResponse preview(@RequestParam UUID shipperId,
                                            @Valid @RequestBody FirstMilePlanRequest request) {
        return planningService.preview(shipperId, request);
    }

    @GetMapping("/vehicles")
    public List<Vehicle> availableVehicles(@RequestParam UUID shipperId) {
        return sessionService.availableVehicles(shipperId);
    }

    @PostMapping("/sessions/start")
    public PickupSessionResponse start(@RequestParam UUID shipperId,
                                       @Valid @RequestBody FirstMilePlanRequest request) {
        return sessionService.start(shipperId, request);
    }

    @PostMapping("/pickup-requests/{pickupRequestId}/confirm-pickup")
    public PickupConfirmationResponse confirmPickup(
            @PathVariable UUID pickupRequestId,
            @RequestParam UUID shipperId) {
        return confirmationService.confirm(pickupRequestId, shipperId);
    }

    @GetMapping("/pickup-requests/{pickupRequestId}/manifest")
    public PickupManifestResponse manifest(@PathVariable UUID pickupRequestId,
                                           @RequestParam UUID shipperId) {
        return confirmationService.manifest(pickupRequestId, shipperId);
    }
}
