package com.karnoorder.repository.projection;

import com.karnoorder.enums.OrderStatus;
import com.karnoorder.enums.PaymentMethod;
import com.karnoorder.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class CustomerOrderProjection {
    private UUID id;
    private UUID checkoutId;
    private UUID shopId;
    private String shopName;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OffsetDateTime createdAt;
    private Integer totalItems;
}
