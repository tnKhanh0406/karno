package com.karnofleet.service;

import com.karnofleet.dto.request.LocationRequest;
import com.karnofleet.dto.response.FirstMilePreviewResponse;

import java.util.UUID;

public interface FirstMilePlanningService {
    FirstMilePreviewResponse preview(UUID shipperUserId, LocationRequest request);
}
