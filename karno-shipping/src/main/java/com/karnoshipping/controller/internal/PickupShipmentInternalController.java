package com.karnoshipping.controller.internal;

import com.karnoshipping.dto.request.PickupShipmentAssignmentRequest;
import com.karnoshipping.dto.response.PickupShipmentSnapshot;
import com.karnoshipping.service.PickupShipmentAssignmentService;
import com.karnoshipping.service.PickupPlanningService;
import com.karnoshipping.dto.request.PickupPlanningRequest;
import com.karnoshipping.dto.request.PickupConfirmationRequest;
import com.karnoshipping.service.PickupConfirmationService;
import com.karnoshipping.dto.response.PickupManifestItemResponse;
import com.karnoshipping.repository.ShipmentRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/shipments/pickup-requests")
@RequiredArgsConstructor
public class PickupShipmentInternalController {
    private final PickupShipmentAssignmentService assignmentService;
    private final PickupPlanningService planningService;
    private final PickupConfirmationService confirmationService;
    private final ShipmentRepo shipmentRepo;

    @PostMapping("/{pickupRequestId}/claim")
    public List<PickupShipmentSnapshot> claim(
            @PathVariable UUID pickupRequestId,
            @Valid @RequestBody PickupShipmentAssignmentRequest request) {
        return assignmentService.claim(pickupRequestId, request);
    }

    @PutMapping("/{pickupRequestId}/shipments")
    public List<PickupShipmentSnapshot> replace(
            @PathVariable UUID pickupRequestId,
            @Valid @RequestBody PickupShipmentAssignmentRequest request) {
        return assignmentService.replace(pickupRequestId, request);
    }

    @PostMapping("/{pickupRequestId}/release")
    public void release(@PathVariable UUID pickupRequestId, @RequestParam UUID shopId) {
        assignmentService.release(pickupRequestId, shopId);
    }

    @PostMapping("/planning")
    public void plan(@Valid @RequestBody PickupPlanningRequest request) {
        planningService.plan(request.pickupRequestIds());
    }

    @PostMapping("/planning/revert")
    public void unplan(@Valid @RequestBody PickupPlanningRequest request) {
        planningService.unplan(request.pickupRequestIds());
    }

    @PostMapping("/{pickupRequestId}/confirm-pickup")
    public void confirmPickup(@PathVariable UUID pickupRequestId,
                              @Valid @RequestBody PickupConfirmationRequest request) {
        confirmationService.confirm(pickupRequestId, request.shipmentIds());
    }

    @PostMapping("/{pickupRequestId}/confirm-pickup/revert")
    public void revertPickup(@PathVariable UUID pickupRequestId,
                             @Valid @RequestBody PickupConfirmationRequest request) {
        confirmationService.revert(pickupRequestId, request.shipmentIds());
    }

    @GetMapping("/{pickupRequestId}/manifest")
    public List<PickupManifestItemResponse> manifest(@PathVariable UUID pickupRequestId) {
        return shipmentRepo.findByPickupRequestId(pickupRequestId).stream()
                .map(PickupManifestItemResponse::from).toList();
    }
}
