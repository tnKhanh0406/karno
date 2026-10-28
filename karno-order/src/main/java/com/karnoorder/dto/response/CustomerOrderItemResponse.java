package com.karnoorder.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerOrderItemResponse(
        UUID id,
        UUID orderId,
        UUID productId,
        UUID variantId,
        String productName,
        String variantName,
        String productImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {
}
