package com.karnofleet.controller;

import com.karnofleet.dto.request.LastMileOrder;
import com.karnofleet.dto.response.LastMileClustersResponse;
import com.karnofleet.service.LastMileClusteringService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/last-mile/clusters")
public class LastMileClusteringController {
    private final LastMileClusteringService service;

    public LastMileClusteringController(LastMileClusteringService service) {
        this.service = service;
    }

    @GetMapping("/demo")
    public LastMileClustersResponse demo(@RequestParam(defaultValue = "200") double maxDiameterMeters) {
        return service.demo(maxDiameterMeters);
    }

    @PostMapping
    public LastMileClustersResponse cluster(@RequestBody List<LastMileOrder> orders,
            @RequestParam(defaultValue = "200") double maxDiameterMeters) {
        return service.cluster(orders, maxDiameterMeters);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidInput(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
