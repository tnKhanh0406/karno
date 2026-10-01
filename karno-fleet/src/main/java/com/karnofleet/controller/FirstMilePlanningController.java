package com.karnofleet.controller;

import com.karnofleet.dto.request.LocationRequest;
import com.karnofleet.dto.response.FirstMilePreviewResponse;
import com.karnofleet.service.FirstMilePlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/first-mile")
@RequiredArgsConstructor
public class FirstMilePlanningController {

    private final FirstMilePlanningService planningService;

    @PostMapping("/preview")
    public FirstMilePreviewResponse preview(@RequestBody LocationRequest request) {
        UUID userId = UUID.fromString("80000000-0000-0000-0000-000000000007");
        return planningService.preview(userId, request);
    }
}
