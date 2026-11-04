package com.karnofleet.dto.response;

import com.karnofleet.entity.PickupRequest;
import com.karnofleet.enums.PickupRequestStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record SessionPickupRequestResponse(
        UUID id, String requestCode, String shopName, String pickupAddress,
        BigDecimal pickupLatitude, BigDecimal pickupLongitude, PickupRequestStatus status
) {
    public static SessionPickupRequestResponse from(PickupRequest value) {
        return new SessionPickupRequestResponse(value.getId(), value.getRequestCode(),
                value.getShopName(), value.getPickupAddress(), value.getPickupLatitude(),
                value.getPickupLongitude(), value.getStatus());
    }
}
