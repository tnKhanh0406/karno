package com.karnofleet.service;

import com.karnofleet.dto.response.FirstMileContext;

import java.time.LocalDateTime;
import java.util.UUID;

public interface FirstMileContextService {
    FirstMileContext getContext(UUID shipperUserId, LocalDateTime now);
}
