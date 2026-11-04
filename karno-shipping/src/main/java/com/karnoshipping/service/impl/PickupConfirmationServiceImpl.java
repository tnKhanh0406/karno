package com.karnoshipping.service.impl;

import com.karnoshipping.entity.Shipment;
import com.karnoshipping.enums.ShipmentStatus;
import com.karnoshipping.repository.ShipmentRepo;
import com.karnoshipping.repository.ShipmentStatusHistoryRepo;
import com.karnoshipping.service.PickupConfirmationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PickupConfirmationServiceImpl implements PickupConfirmationService {
    private final ShipmentRepo shipmentRepo;
    private final ShipmentStatusHistoryRepo historyRepo;

    @Override
    @Transactional
    public void confirm(UUID pickupRequestId, List<UUID> requestedShipmentIds) {
        List<UUID> expectedIds = distinct(requestedShipmentIds);
        List<Shipment> shipments = shipmentRepo.findByPickupRequestIdForUpdate(pickupRequestId);
        validateExactManifest(expectedIds, shipments);

        boolean alreadyPickedUp = shipments.stream()
                .allMatch(s -> s.getStatus() == ShipmentStatus.PICKED_UP);
        if (alreadyPickedUp) return;
        if (shipments.stream().anyMatch(s -> s.getStatus() != ShipmentStatus.PICKUP_PLANNED)) {
            throw conflict("All shipments must be PICKUP_PLANNED before confirmation");
        }
        if (shipmentRepo.markPickedUp(pickupRequestId, expectedIds) != shipments.size()) {
            throw conflict("Shipment state changed during pickup confirmation");
        }
        shipments.forEach(s -> insertHistory(s.getId(), "PICKUP_PLANNED", "PICKED_UP",
                "Shipper confirmed full pickup request"));
    }

    @Override
    @Transactional
    public void revert(UUID pickupRequestId, List<UUID> requestedShipmentIds) {
        List<UUID> expectedIds = distinct(requestedShipmentIds);
        List<Shipment> shipments = shipmentRepo.findByPickupRequestIdForUpdate(pickupRequestId);
        validateExactManifest(expectedIds, shipments);
        List<Shipment> changing = shipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.PICKED_UP).toList();
        if (changing.isEmpty()) return;
        if (shipments.stream().anyMatch(s -> s.getStatus() != ShipmentStatus.PICKED_UP
                && s.getStatus() != ShipmentStatus.PICKUP_PLANNED)) {
            throw conflict("Shipment cannot be reverted to PICKUP_PLANNED");
        }
        if (shipmentRepo.revertPickedUp(pickupRequestId, expectedIds) != changing.size()) {
            throw conflict("Shipment state changed during pickup compensation");
        }
        changing.forEach(s -> insertHistory(s.getId(), "PICKED_UP", "PICKUP_PLANNED",
                "Pickup confirmation was compensated"));
    }

    @Override
    @Transactional
    public void completeSession(List<UUID> pickedUpPickupRequestIds,
                                List<UUID> unpickedPickupRequestIds) {
        List<UUID> picked = distinctAllowEmpty(pickedUpPickupRequestIds);
        List<UUID> unpicked = distinctAllowEmpty(unpickedPickupRequestIds);
        Set<UUID> overlap = new HashSet<>(picked);
        overlap.retainAll(unpicked);
        if (!overlap.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A pickup request cannot be both picked up and unpicked");
        }

        if (!picked.isEmpty()) {
            List<Shipment> shipments = shipmentRepo.findByPickupRequestIdsForUpdate(picked);
            validateRequestCoverage(picked, shipments);
            if (shipments.stream().anyMatch(s -> s.getStatus() != ShipmentStatus.PICKED_UP
                    && s.getStatus() != ShipmentStatus.AT_ORIGIN_HUB)) {
                throw conflict("Picked-up request contains shipment that cannot arrive at origin hub");
            }
            List<Shipment> changing = shipments.stream()
                    .filter(s -> s.getStatus() == ShipmentStatus.PICKED_UP).toList();
            if (shipmentRepo.markAtOriginHub(picked) != changing.size()) {
                throw conflict("Shipment state changed during origin hub confirmation");
            }
            changing.forEach(s -> insertHistory(s.getId(), "PICKED_UP", "AT_ORIGIN_HUB",
                    "First-mile session completed at origin hub"));
        }

        if (!unpicked.isEmpty()) {
            List<Shipment> shipments = shipmentRepo.findByPickupRequestIdsForUpdate(unpicked);
            validateRequestCoverage(unpicked, shipments);
            if (shipments.stream().anyMatch(s -> s.getStatus() != ShipmentStatus.PICKUP_PLANNED
                    && s.getStatus() != ShipmentStatus.PICKUP_REQUESTED)) {
                throw conflict("Unpicked request contains shipment that cannot be released");
            }
            List<Shipment> changing = shipments.stream()
                    .filter(s -> s.getStatus() == ShipmentStatus.PICKUP_PLANNED).toList();
            if (shipmentRepo.revertPickupPlanned(unpicked) != changing.size()) {
                throw conflict("Shipment state changed while releasing unpicked requests");
            }
            changing.forEach(s -> insertHistory(s.getId(), "PICKUP_PLANNED", "PICKUP_REQUESTED",
                    "Unpicked request released when first-mile session completed"));
        }
    }

    private void validateRequestCoverage(List<UUID> requestIds, List<Shipment> shipments) {
        Set<UUID> found = shipments.stream().map(Shipment::getPickupRequestId)
                .collect(java.util.stream.Collectors.toSet());
        if (!found.containsAll(requestIds)) {
            throw conflict("One or more pickup requests have no shipment data");
        }
    }

    private List<UUID> distinctAllowEmpty(List<UUID> ids) {
        if (ids == null || ids.stream().anyMatch(Objects::isNull)
                || new HashSet<>(ids).size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pickup request ids must be unique and non-null");
        }
        return List.copyOf(ids);
    }

    private void validateExactManifest(List<UUID> expectedIds, List<Shipment> shipments) {
        Set<UUID> actualIds = shipments.stream().map(Shipment::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (shipments.isEmpty() || actualIds.size() != expectedIds.size()
                || !actualIds.equals(Set.copyOf(expectedIds))) {
            throw conflict("Pickup manifest does not match Shipping data");
        }
    }

    private List<UUID> distinct(List<UUID> ids) {
        if (ids == null || ids.isEmpty() || new HashSet<>(ids).size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "shipmentIds must be non-empty and unique");
        }
        return List.copyOf(ids);
    }

    private void insertHistory(UUID shipmentId, String from, String to, String reason) {
        if (historyRepo.insertTransition(UUID.randomUUID(), shipmentId, from, to, reason) != 1) {
            throw new IllegalStateException("Cannot insert shipment pickup history");
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
