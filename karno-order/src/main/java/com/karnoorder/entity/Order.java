package com.karnoorder.entity;

import com.karnoorder.enums.OrderStatus;
import com.karnoorder.enums.PaymentMethod;
import com.karnoorder.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private UUID id;
    private UUID customerId;
    private UUID checkoutId;
    private String receiverName;
    private String receiverAddress;
    private String receiverPhone;
    private UUID shopId;
    private String shopName;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OffsetDateTime deliverySlotStart;
    private OffsetDateTime deliverySlotEnd;
    private String note;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}