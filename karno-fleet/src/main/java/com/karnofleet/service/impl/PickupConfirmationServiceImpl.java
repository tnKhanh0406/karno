package com.karnofleet.service.impl;

import com.karnofleet.client.ShippingClient;
import com.karnofleet.dto.response.PickupConfirmationResponse;
import com.karnofleet.dto.response.PickupManifestResponse;
import com.karnofleet.entity.PickupRequest;
import com.karnofleet.entity.PickupSession;
import com.karnofleet.enums.PickupRequestStatus;
import com.karnofleet.repository.FirstMileSessionRepo;
import com.karnofleet.repository.PickupRequestRepo;
import com.karnofleet.service.PickupConfirmationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PickupConfirmationServiceImpl implements PickupConfirmationService {
    private final PickupRequestRepo pickupRequestRepo;
    private final FirstMileSessionRepo sessionRepo;
    private final ShippingClient shippingClient;

    @Override
    public PickupManifestResponse manifest(UUID pickupRequestId, UUID shipperId) {
        PickupRequest request = pickupRequestRepo.findById(pickupRequestId);
        if (request == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pickup request not found");
        if (!Objects.equals(shipperId, request.getAssignedShipperId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pickup request is not assigned to this shipper");
        return new PickupManifestResponse(pickupRequestId, request.getRequestCode(),
                request.getStatus(), shippingClient.manifest(pickupRequestId));
    }

    @Override
    @Transactional
    public PickupConfirmationResponse confirm(UUID pickupRequestId, UUID shipperId) {
        PickupRequest request = pickupRequestRepo.findById(pickupRequestId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pickup request not found");
        }
        if (!Objects.equals(shipperId, request.getAssignedShipperId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Pickup request is not assigned to this shipper");
        }
        List<UUID> shipmentIds = pickupRequestRepo.findShipmentIds(pickupRequestId);
        if (shipmentIds.isEmpty()) {
            throw conflict("Pickup request has no shipment manifest");
        }
        if (request.getStatus() == PickupRequestStatus.PICKED_UP) {
            return new PickupConfirmationResponse(
                    pickupRequestId, PickupRequestStatus.PICKED_UP, shipmentIds, true);
        }
        if (request.getStatus() != PickupRequestStatus.PLANNING) {
            throw conflict("Only PLANNING pickup request can be confirmed");
        }
        PickupSession session = sessionRepo.findActiveByShipper(shipperId);
        if (session == null || !Objects.equals(session.getId(), request.getPickupSessionId())) {
            throw conflict("Pickup request does not belong to shipper's active session");
        }

        Map<String, List<UUID>> manifest = Map.of("shipmentIds", shipmentIds);
        boolean shippingConfirmed = false;
        try {
            shippingClient.confirmPickup(pickupRequestId, manifest);
            shippingConfirmed = true;
            if (pickupRequestRepo.markPickedUp(
                    pickupRequestId, shipperId, session.getId()) != 1) {
                PickupRequest current = pickupRequestRepo.findById(pickupRequestId);
                if (current != null && current.getStatus() == PickupRequestStatus.PICKED_UP) {
                    return new PickupConfirmationResponse(
                            pickupRequestId, PickupRequestStatus.PICKED_UP, shipmentIds, true);
                }
                throw conflict("Pickup request state changed during confirmation");
            }
            return new PickupConfirmationResponse(
                    pickupRequestId, PickupRequestStatus.PICKED_UP, shipmentIds, false);
        } catch (RuntimeException ex) {
            if (shippingConfirmed) {
                try {
                    shippingClient.revertPickup(pickupRequestId, manifest);
                } catch (RuntimeException compensationError) {
                    ex.addSuppressed(compensationError);
                }
            }
            throw ex;
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
