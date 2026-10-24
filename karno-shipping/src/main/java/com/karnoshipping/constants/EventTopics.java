package com.karnoshipping.constants;

public final class EventTopics {
    private EventTopics() {}

    public static final String SHIPMENT_CREATED = "shipping.shipment.created";
    public static final String SHIPMENT_READY_FOR_PICKUP = "shipping.shipment.ready-for-pickup";
    public static final String SHIPMENT_ADDED_TO_PICKUP_REQUEST = "shipping.shipment.added-to-pickup-request";
    public static final String SHIPMENT_REMOVED_FROM_PICKUP_REQUEST = "shipping.shipment.removed-from-pickup-request";
}
