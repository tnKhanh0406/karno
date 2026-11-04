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
import org.springframework.http.ResponseEntity;
import com.karnofleet.dto.response.PickupRequestResponse;

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

    @GetMapping("/sessions/active")
    public ResponseEntity<PickupSessionResponse> active(@RequestParam UUID shipperId) {
        PickupSessionResponse session = sessionService.findActive(shipperId);
        return session == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{sessionId}/replan")
    public PickupSessionResponse replan(@PathVariable UUID sessionId,
                                        @RequestParam UUID shipperId,
                                        @Valid @RequestBody FirstMilePlanRequest request) {
        return sessionService.replan(shipperId, sessionId, request);
    }

    @GetMapping("/sessions/{sessionId}/new-pickup-requests")
    public List<PickupRequestResponse> newPickupRequests(@PathVariable UUID sessionId,
                                                         @RequestParam UUID shipperId) {
        return sessionService.findNewRequests(shipperId, sessionId);
    }

    @GetMapping("/map-pickup-requests")
    public List<PickupRequestResponse> mapPickupRequests(@RequestParam UUID shipperId) {
        return sessionService.findMapRequests(shipperId);
    }

    @PostMapping("/sessions/{sessionId}/pickup-requests/{pickupRequestId}")
    public PickupSessionResponse addPickupRequest(@PathVariable UUID sessionId,
                                                   @PathVariable UUID pickupRequestId,
                                                   @RequestParam UUID shipperId) {
        return sessionService.addRequest(shipperId, sessionId, pickupRequestId);
    }

    @PostMapping("/sessions/{sessionId}/pickup-requests/{pickupRequestId}/defer")
    public void deferPickupRequest(@PathVariable UUID sessionId,
                                   @PathVariable UUID pickupRequestId,
                                   @RequestParam UUID shipperId) {
        sessionService.deferRequest(shipperId, sessionId, pickupRequestId);
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    public PickupSessionResponse cancel(@PathVariable UUID sessionId,
                                        @RequestParam UUID shipperId) {
        return sessionService.cancel(shipperId, sessionId);
    }

    @PostMapping("/sessions/{sessionId}/complete")
    public PickupSessionResponse complete(@PathVariable UUID sessionId,
                                          @RequestParam UUID shipperId) {
        return sessionService.complete(shipperId, sessionId);
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
