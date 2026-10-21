package com.karnoorder.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CheckoutPreviewResponse(
        UUID customerId,
        Receiver receiver,
        List<ShopPreview> shops,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        Instant preparedAt
) {
    public static CheckoutPreviewResponse from(CheckoutContext context) {
        List<ShopPreview> shops = context.orders()
                .stream()
                .map(ShopPreview::from)
                .toList();

        BigDecimal subtotal = shops.stream()
                .map(ShopPreview::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = shops.stream()
                .map(shop -> shop.shipping().shippingFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CheckoutPreviewResponse(
                context.customerId(),
                Receiver.from(context.receiver()),
                List.copyOf(shops),
                subtotal,
                shippingFee,
                subtotal.add(shippingFee),
                context.preparedAt()
        );
    }

    public record ShopPreview(
            UUID shopId,
            String shopName,
            ShippingSnapshot shipping,
            BigDecimal subtotal,
            BigDecimal totalAmount,
            List<ItemPreview> items
    ) {
        private static ShopPreview from(CheckoutContext.OrderDraft order) {
            return new ShopPreview(
                    order.shopId(),
                    order.shopName(),
                    ShippingSnapshot.from(order.shipping()),
                    order.subtotal(),
                    order.totalAmount(),
                    order.items().stream().map(ItemPreview::from).toList()
            );
        }
    }

    public record ItemPreview(
            UUID cartItemId,
            UUID productId,
            UUID variantId,
            String productName,
            String variantName,
            String sku,
            String productImageUrl,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal subtotal
    ) {
        private static ItemPreview from(CheckoutContext.OrderItemDraft item) {
            return new ItemPreview(
                    item.cartItemId(),
                    item.productId(),
                    item.variantId(),
                    item.productName(),
                    item.variantName(),
                    item.sku(),
                    item.productImageUrl(),
                    item.unitPrice(),
                    item.quantity(),
                    item.subtotal()
            );
        }
    }

    public record Receiver(
            String name,
            String phone,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        private static Receiver from(CheckoutContext.Receiver receiver) {
            return new Receiver(
                    receiver.name(),
                    receiver.phone(),
                    receiver.address(),
                    receiver.latitude(),
                    receiver.longitude()
            );
        }
    }

    public record ShippingSnapshot(
            String shippingMethod,
            BigDecimal shippingFee,
            Integer totalWeightGram,
            Instant estimatedDeliveryAt,
            Pickup pickup
    ) {
        private static ShippingSnapshot from(CheckoutContext.ShippingSnapshot shipping) {
            return new ShippingSnapshot(
                    shipping.shippingMethod(),
                    shipping.shippingFee(),
                    shipping.totalWeightGram(),
                    shipping.estimatedDeliveryAt(),
                    Pickup.from(shipping.pickup())
            );
        }
    }

    public record Pickup(
            String name,
            String phone,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        private static Pickup from(CheckoutContext.Pickup pickup) {
            return new Pickup(
                    pickup.name(),
                    pickup.phone(),
                    pickup.address(),
                    pickup.latitude(),
                    pickup.longitude()
            );
        }
    }
}
