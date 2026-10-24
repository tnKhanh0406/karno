package com.karnoshipping.service.impl;

import com.karnoshipping.constants.EventTopics;
import com.karnoshipping.constants.EventTypes;
import com.karnoshipping.dto.request.PickupShipmentAssignmentRequest;
import com.karnoshipping.dto.response.PickupShipmentSnapshot;
import com.karnoshipping.entity.Shipment;
import com.karnoshipping.enums.ShipmentStatus;
import com.karnoshipping.messaging.payload.PickupRequestAssignmentPayload;
import com.karnoshipping.repository.ShipmentRepo;
import com.karnoshipping.repository.ShipmentStatusHistoryRepo;
import com.karnoshipping.service.OutboxService;
import com.karnoshipping.service.PickupShipmentAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PickupShipmentAssignmentServiceImpl implements PickupShipmentAssignmentService {
    private final ShipmentRepo shipmentRepo;
    private final ShipmentStatusHistoryRepo historyRepo;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public List<PickupShipmentSnapshot> claim(
            UUID pickupRequestId, PickupShipmentAssignmentRequest request) {
        List<UUID> ids = distinctIds(request.shipmentIds());
        List<Shipment> shipments = shipmentRepo.findByIdsForUpdate(ids);
        validateSelection(pickupRequestId, request.shopId(), ids, shipments);

        boolean alreadyClaimed = shipments.stream().allMatch(shipment ->
                shipment.getStatus() == ShipmentStatus.PICKUP_REQUESTED
                        && pickupRequestId.equals(shipment.getPickupRequestId()));
        if (!alreadyClaimed) {
            if (shipmentRepo.assignToPickupRequest(pickupRequestId, request.shopId(), ids)
                    != ids.size()) {
                throw conflict("Shipment state changed while creating pickup request");
            }
            recordTransitions(shipments, ShipmentStatus.READY_FOR_PICKUP,
                    ShipmentStatus.PICKUP_REQUESTED, "Added to pickup request " + pickupRequestId);
            enqueue(EventTypes.SHIPMENT_ADDED_TO_PICKUP_REQUEST,
                    EventTopics.SHIPMENT_ADDED_TO_PICKUP_REQUEST,
                    pickupRequestId, request.shopId(), ids);
        }
        return shipments.stream().map(PickupShipmentSnapshot::from).toList();
    }

    @Override
    @Transactional
    public List<PickupShipmentSnapshot> replace(
            UUID pickupRequestId, PickupShipmentAssignmentRequest request) {
        List<UUID> desiredIds = distinctIds(request.shipmentIds());
        List<Shipment> desired = shipmentRepo.findByIdsForUpdate(desiredIds);
        List<Shipment> current = shipmentRepo.findByPickupRequestIdForUpdate(pickupRequestId);
        validateSelection(pickupRequestId, request.shopId(), desiredIds, desired);
        validateCurrentShop(request.shopId(), current);

        shipmentRepo.releaseFromPickupRequest(pickupRequestId);
        if (shipmentRepo.assignToPickupRequest(pickupRequestId, request.shopId(), desiredIds)
                != desiredIds.size()) {
            throw conflict("Shipment state changed while updating pickup request");
        }
        Set<UUID> desiredSet = Set.copyOf(desiredIds);
        current.stream().filter(s -> !desiredSet.contains(s.getId())).forEach(shipment ->
                insertHistory(shipment, ShipmentStatus.PICKUP_REQUESTED,
                        ShipmentStatus.READY_FOR_PICKUP, "Removed from pickup request " + pickupRequestId));
        Set<UUID> currentSet = current.stream().map(Shipment::getId)
                .collect(java.util.stream.Collectors.toSet());
        desired.stream().filter(s -> !currentSet.contains(s.getId())).forEach(shipment ->
                insertHistory(shipment, ShipmentStatus.READY_FOR_PICKUP,
                        ShipmentStatus.PICKUP_REQUESTED, "Added to pickup request " + pickupRequestId));
        enqueue(EventTypes.SHIPMENT_ADDED_TO_PICKUP_REQUEST,
                EventTopics.SHIPMENT_ADDED_TO_PICKUP_REQUEST,
                pickupRequestId, request.shopId(), desiredIds);
        return desired.stream().map(PickupShipmentSnapshot::from).toList();
    }

    @Override
    @Transactional
    public void release(UUID pickupRequestId, UUID shopId) {
        List<Shipment> current = shipmentRepo.findByPickupRequestIdForUpdate(pickupRequestId);
        if (current.isEmpty()) {
            return;
        }
        validateCurrentShop(shopId, current);
        if (shipmentRepo.releaseFromPickupRequest(pickupRequestId) != current.size()) {
            throw conflict("Shipment state changed while cancelling pickup request");
        }
        recordTransitions(current, ShipmentStatus.PICKUP_REQUESTED,
                ShipmentStatus.READY_FOR_PICKUP, "Pickup request cancelled " + pickupRequestId);
        enqueue(EventTypes.SHIPMENT_REMOVED_FROM_PICKUP_REQUEST,
                EventTopics.SHIPMENT_REMOVED_FROM_PICKUP_REQUEST,
                pickupRequestId, shopId, current.stream().map(Shipment::getId).toList());
    }

    private void validateSelection(UUID requestId, UUID shopId, List<UUID> ids,
                                   List<Shipment> shipments) {
        if (shipments.size() != ids.size()) {
            throw conflict("One or more shipments do not exist");
        }
        for (Shipment shipment : shipments) {
            boolean ready = shipment.getStatus() == ShipmentStatus.READY_FOR_PICKUP
                    && shipment.getPickupRequestId() == null;
            boolean ownedByRequest = shipment.getStatus() == ShipmentStatus.PICKUP_REQUESTED
                    && requestId.equals(shipment.getPickupRequestId());
            if (!shopId.equals(shipment.getShopId()) || (!ready && !ownedByRequest)) {
                throw conflict("Shipment is not available for this shop: " + shipment.getId());
            }
        }
    }

    private void validateCurrentShop(UUID shopId, List<Shipment> shipments) {
        if (shipments.stream().anyMatch(s -> !Objects.equals(shopId, s.getShopId()))) {
            throw conflict("Pickup request contains shipment of another shop");
        }
    }

    private List<UUID> distinctIds(List<UUID> ids) {
        List<UUID> result = List.copyOf(new LinkedHashSet<>(ids));
        if (result.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "shipmentIds must not contain duplicates");
        }
        return result;
    }

    private void recordTransitions(List<Shipment> shipments, ShipmentStatus from,
                                   ShipmentStatus to, String reason) {
        shipments.forEach(shipment -> insertHistory(shipment, from, to, reason));
    }

    private void insertHistory(Shipment shipment, ShipmentStatus from,
                               ShipmentStatus to, String reason) {
        if (historyRepo.insertTransition(UUID.randomUUID(), shipment.getId(),
                from.name(), to.name(), reason) != 1) {
            throw new IllegalStateException("Cannot insert shipment history");
        }
    }

    private void enqueue(String type, String topic, UUID requestId,
                         UUID shopId, List<UUID> shipmentIds) {
        outboxService.enqueue("PICKUP_REQUEST", requestId, type, topic,
                new PickupRequestAssignmentPayload(requestId, shopId, shipmentIds));
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
