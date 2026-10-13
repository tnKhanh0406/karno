package com.karnoorder.dto.response;

import com.karnoorder.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CheckoutContext(
        UUID checkoutId,
        UUID customerId,
        Receiver receiver,
        PaymentMethod paymentMethod,
        String note,
        Instant deliverySlotStart,
        Instant deliverySlotEnd,
        Instant preparedAt,
        List<OrderDraft> orders
) {

    public record Receiver(
            String name,
            String phone,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }

    public record Pickup(
            String name,
            String phone,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }

    public record OrderDraft(
            UUID orderId,
            UUID shopId,
            String shopName,
            ShippingSnapshot shipping,
            BigDecimal subtotal,
            BigDecimal totalAmount,
            List<OrderItemDraft> items
    ) {
    }

    public record ShippingSnapshot(
            String shippingMethod,
            BigDecimal shippingFee,
            Integer totalWeightGram,
            Instant estimatedDeliveryAt,
            Pickup pickup
    ) {
    }

    public record OrderItemDraft(
            UUID orderItemId,
            UUID cartItemId,
            UUID productId,
            UUID variantId,
            String productName,
            String variantName,
            String sku,
            String productImageUrl,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal subtotal,

            Integer weightGram,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm
    ) {
    }
}
