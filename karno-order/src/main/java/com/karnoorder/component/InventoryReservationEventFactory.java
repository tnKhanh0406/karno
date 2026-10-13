package com.karnoorder.component;

import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.messaging.payload.InventoryReservationRequestedPayload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryReservationEventFactory {

    public InventoryReservationRequestedPayload build(CheckoutContext context) {
        List<InventoryReservationRequestedPayload.Item> items =
                context.orders()
                        .stream()
                        .flatMap(order ->
                                order.items()
                                        .stream()
                                        .map(item ->
                                                new InventoryReservationRequestedPayload.Item(
                                                        order.orderId(),
                                                        item.orderItemId(),
                                                        item.variantId(),
                                                        item.quantity()
                                                )
                                        )
                        )
                        .toList();

        if (items.isEmpty()) {
            throw new IllegalStateException("Checkout has no items");
        }

        return new InventoryReservationRequestedPayload(
                context.checkoutId(),
                items
        );
    }
}