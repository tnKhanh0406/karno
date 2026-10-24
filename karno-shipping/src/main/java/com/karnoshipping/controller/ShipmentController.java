package com.karnoshipping.controller;

import com.karnoshipping.dto.response.ReadyShipmentResponse;
import com.karnoshipping.service.ShipmentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {
    private final ShipmentQueryService shipmentQueryService;

    @GetMapping("/ready-for-pickup")
    public List<ReadyShipmentResponse> getReadyForPickup(@RequestParam UUID shopId) {
        return shipmentQueryService.getReadyForPickup(shopId);
    }
}
