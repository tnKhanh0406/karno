package com.karnofleet.client;

import com.karnofleet.client.dto.PickupShipmentAssignmentRequest;
import com.karnofleet.client.dto.PickupShipmentSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import com.karnofleet.client.dto.PickupManifestItem;

@FeignClient(name = "fleet-shipping-client", url = "${services.karno-shipping.base-url}")
public interface ShippingClient {
    @PostMapping("/internal/shipments/pickup-requests/{id}/claim")
    List<PickupShipmentSnapshot> claim(@PathVariable UUID id,
                                       @RequestBody PickupShipmentAssignmentRequest request);

    @PutMapping("/internal/shipments/pickup-requests/{id}/shipments")
    List<PickupShipmentSnapshot> replace(@PathVariable UUID id,
                                         @RequestBody PickupShipmentAssignmentRequest request);

    @PostMapping("/internal/shipments/pickup-requests/{id}/release")
    void release(@PathVariable UUID id, @RequestParam UUID shopId);
    @PostMapping("/internal/shipments/pickup-requests/planning")
    void plan(@RequestBody Map<String, List<UUID>> request);
    @PostMapping("/internal/shipments/pickup-requests/planning/revert")
    void unplan(@RequestBody Map<String, List<UUID>> request);
    @PostMapping("/internal/shipments/pickup-requests/{id}/confirm-pickup")
    void confirmPickup(@PathVariable UUID id, @RequestBody Map<String, List<UUID>> request);
    @PostMapping("/internal/shipments/pickup-requests/{id}/confirm-pickup/revert")
    void revertPickup(@PathVariable UUID id, @RequestBody Map<String, List<UUID>> request);
    @GetMapping("/internal/shipments/pickup-requests/{id}/manifest")
    List<PickupManifestItem> manifest(@PathVariable UUID id);
}
