package com.karnofleet.dto.response;

import com.karnofleet.enums.UnassignedReason;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UnassignedRequestResponse(
        UUID requestId,
        String requestCode,
        UnassignedReason reason
) {
}
