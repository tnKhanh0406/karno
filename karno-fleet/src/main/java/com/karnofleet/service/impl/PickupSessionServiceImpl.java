package com.karnofleet.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnofleet.client.ShippingClient;
import com.karnofleet.dto.request.FirstMilePlanRequest;
import com.karnofleet.dto.response.FirstMilePreviewResponse;
import com.karnofleet.dto.response.PickupSessionResponse;
import com.karnofleet.entity.PickupSession;
import com.karnofleet.entity.ServiceZone;
import com.karnofleet.entity.Vehicle;
import com.karnofleet.repository.FirstMileSessionRepo;
import com.karnofleet.repository.PickupRequestRepo;
import com.karnofleet.repository.VehicleRepo;
import com.karnofleet.service.FirstMilePlanningService;
import com.karnofleet.service.PickupSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        if (sessionRepo.findActiveByShipper(shipperId) != null)
            throw new IllegalStateException("Shipper already has an active pickup session");
        ServiceZone zone = requireZone(shipperId);
        FirstMilePreviewResponse preview = planningService.preview(shipperId, request);
        if (vehicleRepo.acquire(request.vehicleId(), shipperId, zone.getHubId()) != 1)
            throw new IllegalStateException("Vehicle is no longer available");
        UUID sessionId = UUID.randomUUID();
        PickupSession session = PickupSession.builder().id(sessionId).shipperId(shipperId)
                .hubId(zone.getHubId()).zoneId(zone.getId()).vehicleId(request.vehicleId())
                .startLatitude(request.latitude()).startLongitude(request.longitude())
                .plannedStartAt(request.plannedStartAt()).desiredReturnAt(request.desiredReturnAt())
                .status("ACTIVE").routePlan(toJson(preview)).build();
        if (sessionRepo.insert(session) != 1) throw new IllegalStateException("Cannot create pickup session");
        pickupRequestRepo.claimOpenByHubAndZone(zone.getHubId(), zone.getId(), sessionId, shipperId);
        List<UUID> claimedIds = pickupRequestRepo.findIdsBySession(sessionId);
        if (!claimedIds.isEmpty()) {
            shippingClient.plan(Map.of("pickupRequestIds", claimedIds));
        }
        return new PickupSessionResponse(sessionId, "ACTIVE", request.vehicleId(),
                zone.getHubId(), zone.getId(), claimedIds, preview);
    }

    private ServiceZone requireZone(UUID shipperId) {
        ServiceZone zone = sessionRepo.findActiveZoneByShipper(shipperId);
        if (zone == null) throw new IllegalStateException("Shipper has no active zone");
        return zone;
    }
    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize route preview", ex); }
    }
}
