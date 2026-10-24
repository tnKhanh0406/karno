package com.karnofleet.service;
import com.karnofleet.dto.request.FirstMilePlanRequest;
import com.karnofleet.dto.response.PickupSessionResponse;
import com.karnofleet.entity.Vehicle;
import java.util.List;
import java.util.UUID;
public interface PickupSessionService {
    List<Vehicle> availableVehicles(UUID shipperId);
    PickupSessionResponse start(UUID shipperId, FirstMilePlanRequest request);
}
