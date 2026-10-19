package com.karnofleet.controller;

import com.karnofleet.dto.request.LastMileOrder;
import com.karnofleet.dto.response.LastMileLoadResponse;
import com.karnofleet.service.LastMileLoadingService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/last-mile/load")
@RequiredArgsConstructor
public class LastMileLoadingController {
    private final LastMileLoadingService service;

    @PostMapping
    public LastMileLoadResponse select(
            @RequestParam UUID shipperUserId,
            @RequestParam(defaultValue = "200") double maxDiameterMeters,
            @RequestBody List<LastMileOrder> orders
    ) {
        return service.select(shipperUserId, orders, maxDiameterMeters);
    }

    @PostMapping("/demo")
    public LastMileLoadResponse selectDemo(
            @RequestParam UUID shipperUserId,
            @RequestParam(defaultValue = "200") double maxDiameterMeters
    ) {
        return service.selectDemo(shipperUserId, maxDiameterMeters);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidInput(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
