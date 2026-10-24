package com.karnoshipping.service;
import java.util.List;
import java.util.UUID;
public interface PickupPlanningService {
    void plan(List<UUID> pickupRequestIds);
    void unplan(List<UUID> pickupRequestIds);
}
