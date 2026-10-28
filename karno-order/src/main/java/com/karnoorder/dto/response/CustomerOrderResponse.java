package com.karnoorder.dto.response;

import com.karnoorder.enums.OrderStatus;
import com.karnoorder.enums.PaymentMethod;
import com.karnoorder.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CustomerOrderResponse(
        UUID id,
        UUID checkoutId,
        UUID shopId,
        String shopName,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        OffsetDateTime createdAt,
        Integer totalItems,
        List<CustomerOrderItemResponse> items
) {
}
