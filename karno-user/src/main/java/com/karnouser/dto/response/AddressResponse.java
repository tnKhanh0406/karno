package com.karnouser.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AddressResponse(
        UUID addressId,
        String receiverName,
        String receiverPhone,
        String fullAddress,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
