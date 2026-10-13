package com.karnoorder.component;

import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.messaging.payload.ShipmentCreationRequestedPayload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShipmentCreationEventFactory {

    public ShipmentCreationRequestedPayload build(
            CheckoutContext context,
            CheckoutContext.OrderDraft order
    ) {

        if (context.receiver() == null) {
            throw new IllegalStateException(
                    "Missing receiver in CheckoutContext"
            );
        }

        if (order.shipping() == null
                || order.shipping().pickup() == null) {

            throw new IllegalStateException(
                    "Missing shipping snapshot for orderId="
                            + order.orderId()
            );
        }

        CheckoutContext.Pickup pickup =
                order.shipping().pickup();

        ShipmentCreationRequestedPayload.Pickup pickupPayload =
                new ShipmentCreationRequestedPayload.Pickup(
                        pickup.name(),
                        pickup.phone(),
                        pickup.address(),
                        pickup.latitude(),
                        pickup.longitude()
                );

        CheckoutContext.Receiver receiver =
                context.receiver();

        ShipmentCreationRequestedPayload.Receiver receiverPayload =
                new ShipmentCreationRequestedPayload.Receiver(
                        receiver.name(),
                        receiver.phone(),
                        receiver.address(),
                        receiver.latitude(),
                        receiver.longitude()
                );

        List<ShipmentCreationRequestedPayload.Item> items =
                order.items()
                        .stream()
                        .map(item ->
                                new ShipmentCreationRequestedPayload.Item(
                                        item.orderItemId(),
                                        item.variantId(),
                                        item.quantity(),
                                        item.weightGram(),
                                        item.lengthCm(),
                                        item.widthCm(),
                                        item.heightCm()
                                )
                        )
                        .toList();

        return new ShipmentCreationRequestedPayload(
                context.checkoutId(),
                order.orderId(),
                context.customerId(),
                order.shopId(),
                order.shipping().shippingMethod(),
                order.shipping().shippingFee(),
                order.shipping().totalWeightGram(),
                order.shipping().estimatedDeliveryAt(),
                pickupPayload,
                receiverPayload,
                items
        );
    }
}
