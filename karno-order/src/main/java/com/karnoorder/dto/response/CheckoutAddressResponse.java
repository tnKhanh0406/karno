package com.karnoorder.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutAddressResponse(
        UUID addressId,

        String receiverName,
        String receiverPhone,

        String fullAddress,

        BigDecimal latitude,
        BigDecimal longitude
) {
}
