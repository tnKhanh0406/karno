package com.karnofleet.service;
import com.karnofleet.dto.request.FirstMilePlanRequest;
import com.karnofleet.dto.response.PickupSessionResponse;
import com.karnofleet.entity.Vehicle;
import com.karnofleet.dto.response.PickupRequestResponse;
import java.util.List;
import java.util.UUID;
public interface PickupSessionService {
    List<Vehicle> availableVehicles(UUID shipperId);
    PickupSessionResponse start(UUID shipperId, FirstMilePlanRequest request);
    PickupSessionResponse findActive(UUID shipperId);
    PickupSessionResponse replan(UUID shipperId, UUID sessionId, FirstMilePlanRequest request);
    List<PickupRequestResponse> findNewRequests(UUID shipperId, UUID sessionId);
    List<PickupRequestResponse> findMapRequests(UUID shipperId);
    PickupSessionResponse addRequest(UUID shipperId, UUID sessionId, UUID pickupRequestId);
    void deferRequest(UUID shipperId, UUID sessionId, UUID pickupRequestId);
    PickupSessionResponse cancel(UUID shipperId, UUID sessionId);
    PickupSessionResponse complete(UUID shipperId, UUID sessionId);
}
