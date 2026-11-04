package com.karnofleet.service;

import com.karnofleet.dto.request.FirstMilePlanRequest;
import com.karnofleet.dto.response.FirstMilePreviewResponse;

import java.util.UUID;

public interface FirstMilePlanningService {
    FirstMilePreviewResponse preview(UUID shipperUserId, FirstMilePlanRequest request);
    FirstMilePreviewResponse previewSession(UUID shipperUserId, UUID sessionId,
                                            FirstMilePlanRequest request);
}
