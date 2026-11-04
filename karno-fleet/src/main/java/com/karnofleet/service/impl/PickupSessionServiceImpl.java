package com.karnofleet.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnofleet.client.ShippingClient;
import com.karnofleet.dto.request.FirstMilePlanRequest;
import com.karnofleet.dto.response.*;
import com.karnofleet.entity.PickupRequest;
import com.karnofleet.entity.PickupSession;
import com.karnofleet.entity.ServiceZone;
import com.karnofleet.entity.Vehicle;
import com.karnofleet.enums.PickupRequestStatus;
import com.karnofleet.repository.FirstMileSessionRepo;
import com.karnofleet.repository.PickupRequestRepo;
import com.karnofleet.repository.VehicleRepo;
import com.karnofleet.service.FirstMilePlanningService;
import com.karnofleet.service.PickupSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service @RequiredArgsConstructor
public class PickupSessionServiceImpl implements PickupSessionService {
    private final FirstMileSessionRepo sessionRepo;
    private final VehicleRepo vehicleRepo;
    private final PickupRequestRepo pickupRequestRepo;
    private final FirstMilePlanningService planningService;
    private final ShippingClient shippingClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<Vehicle> availableVehicles(UUID shipperId) {
        ServiceZone zone = requireZone(shipperId);
        return vehicleRepo.findAvailableForShipper(shipperId, zone.getHubId());
    }

    @Override @Transactional
    public PickupSessionResponse start(UUID shipperId, FirstMilePlanRequest request) {
        PickupSession current = sessionRepo.findActiveByShipper(shipperId);
        if (current != null) return response(current);
        ServiceZone zone = requireZone(shipperId);
        FirstMilePreviewResponse preview = planningService.preview(shipperId, request);
        if (vehicleRepo.acquire(request.vehicleId(), shipperId, zone.getHubId()) != 1)
            throw conflict("Vehicle is no longer available; choose another vehicle");
        UUID sessionId = UUID.randomUUID();
        PickupSession session = PickupSession.builder().id(sessionId).shipperId(shipperId)
                .hubId(zone.getHubId()).zoneId(zone.getId()).vehicleId(request.vehicleId())
                .startLatitude(request.latitude()).startLongitude(request.longitude())
                .plannedStartAt(request.plannedStartAt()).desiredReturnAt(request.desiredReturnAt())
                .status("ACTIVE").routePlan(toJson(preview)).build();
        if (sessionRepo.insert(session) != 1) throw conflict("Cannot create pickup session");
        pickupRequestRepo.claimOpenByHubAndZone(zone.getHubId(), zone.getId(), sessionId, shipperId);
        List<UUID> claimedIds = pickupRequestRepo.findIdsBySession(sessionId);
        try {
            if (!claimedIds.isEmpty()) shippingClient.plan(Map.of("pickupRequestIds", claimedIds));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Shipping service could not plan pickup requests", ex);
        }
        return response(session, preview);
    }

    @Override
    public PickupSessionResponse findActive(UUID shipperId) {
        PickupSession session = sessionRepo.findActiveByShipper(shipperId);
        return session == null ? null : response(session);
    }

    @Override
    @Transactional
    public PickupSessionResponse replan(UUID shipperId, UUID sessionId, FirstMilePlanRequest request) {
        PickupSession session = requireOwnedSession(shipperId, sessionId, true);
        FirstMilePreviewResponse preview = planningService.previewSession(shipperId, sessionId, request);
        String routePlan = toJson(preview);
        if (sessionRepo.updateRoutePlan(sessionId, routePlan) != 1)
            throw conflict("Pickup session state changed during replan");
        session.setRoutePlan(routePlan);
        return response(session, preview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PickupRequestResponse> findNewRequests(UUID shipperId, UUID sessionId) {
        PickupSession session = requireOwnedSession(shipperId, sessionId, true);
        return pickupRequestRepo.findNewForSession(session.getHubId(), session.getZoneId(), sessionId).stream()
                .map(value -> PickupRequestResponse.from(value,
                        pickupRequestRepo.findShipmentIds(value.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PickupRequestResponse> findMapRequests(UUID shipperId) {
        ServiceZone zone = requireZone(shipperId);
        Map<UUID, PickupRequest> visible = new LinkedHashMap<>();
        PickupSession session = sessionRepo.findActiveByShipper(shipperId);
        if (session != null) {
            pickupRequestRepo.findBySession(session.getId())
                    .forEach(request -> visible.put(request.getId(), request));
        }
        pickupRequestRepo.findOpenByHubAndZone(zone.getHubId(), zone.getId())
                .forEach(request -> visible.putIfAbsent(request.getId(), request));
        return visible.values().stream()
                .map(request -> PickupRequestResponse.from(request,
                        pickupRequestRepo.findShipmentIds(request.getId())))
                .toList();
    }

    @Override
    @Transactional
    public PickupSessionResponse addRequest(UUID shipperId, UUID sessionId, UUID pickupRequestId) {
        PickupSession session = requireOwnedSession(shipperId, sessionId, true);
        if (pickupRequestRepo.claimOne(pickupRequestId, session.getHubId(), session.getZoneId(),
                sessionId, shipperId) != 1) {
            PickupRequest current = pickupRequestRepo.findById(pickupRequestId);
            if (current != null && sessionId.equals(current.getPickupSessionId())
                    && current.getStatus() == PickupRequestStatus.PLANNING) return response(session);
            throw conflict("Pickup request is no longer OPEN or is outside this session zone");
        }
        try {
            shippingClient.plan(Map.of("pickupRequestIds", List.of(pickupRequestId)));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Shipping service could not add pickup request to session", ex);
        }
        return response(session);
    }

    @Override
    @Transactional
    public void deferRequest(UUID shipperId, UUID sessionId, UUID pickupRequestId) {
        PickupSession session = requireOwnedSession(shipperId, sessionId, true);
        PickupRequest request = pickupRequestRepo.findById(pickupRequestId);
        if (request == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Pickup request not found");
        if (request.getStatus() != PickupRequestStatus.OPEN
                || !session.getHubId().equals(request.getTargetHubId())
                || !session.getZoneId().equals(request.getPickupZoneId())) {
            throw conflict("Only an OPEN pickup request in this session zone can be deferred");
        }
        pickupRequestRepo.deferForSession(sessionId, pickupRequestId);
    }

    @Override
    @Transactional
    public PickupSessionResponse cancel(UUID shipperId, UUID sessionId) {
        PickupSession session = requireOwnedSession(shipperId, sessionId, false);
        if ("CANCELLED".equals(session.getStatus())) return response(session);
        if (!"ACTIVE".equals(session.getStatus())) throw conflict("Only ACTIVE session can be cancelled");
        if (pickupRequestRepo.countPickedUpBySession(sessionId) > 0)
            throw conflict("Session already has picked-up requests; return to hub and complete it");
        List<UUID> ids = pickupRequestRepo.findIdsBySession(sessionId);
        if (!ids.isEmpty()) shippingClient.unplan(Map.of("pickupRequestIds", ids));
        pickupRequestRepo.revertSession(sessionId);
        if (vehicleRepo.release(session.getVehicleId()) != 1)
            throw conflict("Session vehicle could not be released");
        if (sessionRepo.markCancelled(sessionId) != 1) throw conflict("Session state changed");
        session.setStatus("CANCELLED");
        return response(session);
    }

    @Override
    @Transactional
    public PickupSessionResponse complete(UUID shipperId, UUID sessionId) {
        PickupSession session = requireOwnedSession(shipperId, sessionId, false);
        if ("COMPLETED".equals(session.getStatus())) return response(session);
        if (!Set.of("ACTIVE", "COMPLETING").contains(session.getStatus()))
            throw conflict("Pickup session cannot be completed from status " + session.getStatus());
        if ("ACTIVE".equals(session.getStatus()) && sessionRepo.markCompleting(sessionId, shipperId) != 1)
            throw conflict("Pickup session state changed during completion");

        List<PickupRequest> requests = pickupRequestRepo.findBySession(sessionId);
        List<UUID> picked = requests.stream().filter(it -> it.getStatus() == PickupRequestStatus.PICKED_UP)
                .map(PickupRequest::getId).toList();
        List<UUID> unpicked = requests.stream().filter(it -> it.getStatus() == PickupRequestStatus.PLANNING)
                .map(PickupRequest::getId).toList();
        try {
            shippingClient.completeSession(Map.of("pickedUpPickupRequestIds", picked,
                    "unpickedPickupRequestIds", unpicked));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Shipping service could not complete first-mile session; retry completion", ex);
        }
        pickupRequestRepo.markPickedUpAtOriginHub(sessionId);
        pickupRequestRepo.revertSession(sessionId);
        if (vehicleRepo.release(session.getVehicleId()) != 1)
            throw conflict("Session vehicle could not be released; retry completion");
        if (sessionRepo.markCompleted(sessionId) != 1)
            throw conflict("Pickup session state changed during completion");
        session.setStatus("COMPLETED");
        return response(session);
    }

    private PickupSession requireOwnedSession(UUID shipperId, UUID sessionId, boolean activeOnly) {
        PickupSession session = sessionRepo.findByIdForUpdate(sessionId);
        if (session == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pickup session not found");
        if (!shipperId.equals(session.getShipperId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Pickup session belongs to another shipper");
        if (activeOnly && !"ACTIVE".equals(session.getStatus()))
            throw conflict("Pickup session is not ACTIVE");
        return session;
    }

    private PickupSessionResponse response(PickupSession session) {
        return response(session, fromJson(session.getRoutePlan()));
    }

    private PickupSessionResponse response(PickupSession session, FirstMilePreviewResponse preview) {
        List<PickupRequest> requests = pickupRequestRepo.findBySession(session.getId());
        return new PickupSessionResponse(session.getId(), session.getStatus(), session.getVehicleId(),
                session.getHubId(), session.getZoneId(),
                requests.stream().map(PickupRequest::getId).toList(), preview,
                requests.stream().map(SessionPickupRequestResponse::from).toList());
    }

    private ServiceZone requireZone(UUID shipperId) {
        ServiceZone zone = sessionRepo.findActiveZoneByShipper(shipperId);
        if (zone == null) throw conflict("Shipper has no active zone");
        return zone;
    }
    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize route preview", ex); }
    }

    private FirstMilePreviewResponse fromJson(String value) {
        if (value == null || value.isBlank()) return null;
        try { return objectMapper.readValue(value, FirstMilePreviewResponse.class); }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot deserialize route preview", ex);
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
