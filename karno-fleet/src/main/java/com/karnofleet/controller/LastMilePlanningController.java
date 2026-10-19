package com.karnofleet.controller;

import com.karnofleet.dto.request.LastMileOrder;
import com.karnofleet.dto.response.LastMilePlanningResponse;
import com.karnofleet.service.LastMilePlanningService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/last-mile/plan")
@RequiredArgsConstructor
public class LastMilePlanningController {
    private final LastMilePlanningService service;

    @PostMapping
    public LastMilePlanningResponse plan(
            @RequestParam UUID shipperUserId,
            @RequestParam(defaultValue = "200") double maxDiameterMeters,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime plannedStartAt,
            @RequestBody List<LastMileOrder> orders
    ) {
        return service.plan(shipperUserId, orders, maxDiameterMeters, plannedStartAt);
    }

    @PostMapping("/demo")
    public LastMilePlanningResponse planDemo(
            @RequestParam UUID shipperUserId,
            @RequestParam(defaultValue = "200") double maxDiameterMeters,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime plannedStartAt
    ) {
        return service.planDemo(shipperUserId, maxDiameterMeters, plannedStartAt);
    }
}
