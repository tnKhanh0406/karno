package com.karnoshipping.constants;

public final class EventTypes {
    private EventTypes() {}

    public static final String SHIPMENT_CREATION_REQUESTED = "SHIPMENT_CREATION_REQUESTED";
    public static final String SHIPMENT_CREATED = "SHIPMENT_CREATED";
    public static final String ORDER_PACKED = "ORDER_PACKED";
    public static final String SHIPMENT_READY_FOR_PICKUP = "SHIPMENT_READY_FOR_PICKUP";
    public static final String SHIPMENT_ADDED_TO_PICKUP_REQUEST = "SHIPMENT_ADDED_TO_PICKUP_REQUEST";
    public static final String SHIPMENT_REMOVED_FROM_PICKUP_REQUEST = "SHIPMENT_REMOVED_FROM_PICKUP_REQUEST";
}
