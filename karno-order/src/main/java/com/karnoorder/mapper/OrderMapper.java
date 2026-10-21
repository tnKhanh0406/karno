package com.karnoorder.mapper;

import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.entity.Order;
import com.karnoorder.entity.OrderItem;
import com.karnoorder.enums.OrderStatus;
import com.karnoorder.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.UUID;

@Component
public class OrderMapper {

    public Order toOrder(
            CheckoutContext context,
            CheckoutContext.OrderDraft draft
    ) {
        Order order = new Order();
        order.setId(draft.orderId());
        order.setCustomerId(context.customerId());
        order.setCheckoutId(context.checkoutId());
        order.setReceiverName(context.receiver().name());
        order.setReceiverAddress(context.receiver().address());
        order.setReceiverPhone(context.receiver().phone());
        order.setShopId(draft.shopId());
        order.setShopName(draft.shopName());
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(draft.subtotal());
        order.setShippingFee(draft.shipping().shippingFee());
        order.setTotalAmount(draft.totalAmount());
        order.setShippingMethod(draft.shipping().shippingMethod());
        order.setPaymentMethod(context.paymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDeliverySlotStart(
                context.deliverySlotStart() == null
                        ? null
                        : context.deliverySlotStart().atOffset(ZoneOffset.UTC)
        );
        order.setDeliverySlotEnd(
                context.deliverySlotEnd() == null
                        ? null
                        : context.deliverySlotEnd().atOffset(ZoneOffset.UTC)
        );
        order.setNote(context.note());
        return order;
    }

    public OrderItem toOrderItem(
            UUID orderId,
            CheckoutContext.OrderItemDraft draft
    ) {
        OrderItem item = new OrderItem();
        item.setId(draft.orderItemId());
        item.setOrderId(orderId);
        item.setProductId(draft.productId());
        item.setVariantId(draft.variantId());
        item.setProductName(draft.productName());
        item.setVariantName(draft.variantName());
        item.setSku(draft.sku());
        item.setProductImageUrl(draft.productImageUrl());
        item.setUnitPrice(draft.unitPrice());
        item.setQuantity(draft.quantity());
        item.setSubtotal(draft.subtotal());
        item.setWeightGram(draft.weightGram());
        item.setLengthCm(draft.lengthCm());
        item.setWidthCm(draft.widthCm());
        item.setHeightCm(draft.heightCm());
        return item;
    }
}
