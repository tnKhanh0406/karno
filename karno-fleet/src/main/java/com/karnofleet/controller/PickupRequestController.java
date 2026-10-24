package com.karnofleet.controller;

import com.karnofleet.entity.PickupRequest;
import com.karnofleet.dto.request.CreatePickupRequest;
import com.karnofleet.dto.request.UpdatePickupRequest;
import com.karnofleet.dto.response.PickupRequestResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.karnofleet.service.PickupRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pickup-requests")
@RequiredArgsConstructor
public class PickupRequestController {

    private final PickupRequestService pickupRequestService;

    @GetMapping
    public List<PickupRequest> getPickupRequests(@RequestParam UUID hubId) {
        return pickupRequestService.findPlanningCandidates(hubId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PickupRequestResponse create(@Valid @RequestBody CreatePickupRequest request) {
        return pickupRequestService.create(request);
    }

    @PutMapping("/{id}")
    public PickupRequestResponse update(@PathVariable UUID id,
                                        @Valid @RequestBody UpdatePickupRequest request) {
        return pickupRequestService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id, @RequestParam UUID sellerId) {
        pickupRequestService.cancel(id, sellerId);
    }

    @GetMapping("/seller")
    public List<PickupRequestResponse> findByShop(@RequestParam UUID shopId,
                                                  @RequestParam UUID sellerId) {
        return pickupRequestService.findByShop(shopId, sellerId);
    }
}
