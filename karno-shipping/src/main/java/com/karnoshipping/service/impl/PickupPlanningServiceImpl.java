package com.karnoshipping.service.impl;

import com.karnoshipping.entity.Shipment;
import com.karnoshipping.enums.ShipmentStatus;
import com.karnoshipping.repository.ShipmentRepo;
import com.karnoshipping.repository.ShipmentStatusHistoryRepo;
import com.karnoshipping.service.PickupPlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service @RequiredArgsConstructor
public class PickupPlanningServiceImpl implements PickupPlanningService {
    private final ShipmentRepo shipmentRepo;
    private final ShipmentStatusHistoryRepo historyRepo;

    @Override @Transactional
    public void plan(List<UUID> requestIds) {
        List<UUID> ids = distinct(requestIds);
        List<Shipment> shipments = shipmentRepo.findByPickupRequestIdsForUpdate(ids);
        Set<UUID> found = new HashSet<>();
        shipments.forEach(s -> found.add(s.getPickupRequestId()));
        if (!found.containsAll(ids) || shipments.stream().anyMatch(s ->
                s.getStatus() != ShipmentStatus.PICKUP_REQUESTED
                        && s.getStatus() != ShipmentStatus.PICKUP_PLANNED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pickup request shipments are not plannable");
        }
        List<Shipment> changing = shipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.PICKUP_REQUESTED).toList();
        if (shipmentRepo.markPickupPlanned(ids) != changing.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shipment state changed during planning");
        }
        changing.forEach(s -> historyRepo.insertTransition(UUID.randomUUID(), s.getId(),
                "PICKUP_REQUESTED", "PICKUP_PLANNED", "First-mile pickup session started"));
    }

    @Override @Transactional
    public void unplan(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Shipment> shipments = shipmentRepo.findByPickupRequestIdsForUpdate(distinct(ids));
        List<Shipment> changing = shipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.PICKUP_PLANNED).toList();
        shipmentRepo.revertPickupPlanned(ids);
        changing.forEach(s -> historyRepo.insertTransition(UUID.randomUUID(), s.getId(),
                "PICKUP_PLANNED", "PICKUP_REQUESTED", "Pickup session start was compensated"));
    }

    private List<UUID> distinct(List<UUID> ids) {
        if (ids == null || ids.isEmpty() || new HashSet<>(ids).size() != ids.size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pickupRequestIds");
        return List.copyOf(ids);
    }
}
