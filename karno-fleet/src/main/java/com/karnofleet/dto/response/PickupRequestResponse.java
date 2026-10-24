package com.karnofleet.dto.response;

import com.karnofleet.entity.PickupRequest;
import com.karnofleet.enums.PickupRequestStatus;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record PickupRequestResponse(
        UUID id, String requestCode, UUID shopId, UUID sellerId, String shopName,
        UUID targetHubId, PickupRequestStatus status, BigDecimal totalWeight,
        BigDecimal totalVolume, LocalTime pickupWindowStart, LocalTime pickupWindowEnd,
        List<UUID> shipmentIds
) {
    public static PickupRequestResponse from(PickupRequest request, List<UUID> shipmentIds) {
        return new PickupRequestResponse(request.getId(), request.getRequestCode(),
                request.getShopId(), request.getSellerId(), request.getShopName(),
                request.getTargetHubId(), request.getStatus(), request.getTotalWeight(),
                request.getTotalVolume(), request.getPickupWindowStart(),
                request.getPickupWindowEnd(), shipmentIds);
    }
}
