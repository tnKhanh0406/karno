package com.karnoorder.constants;

public final class EventTypes {
    private EventTypes() {}

    public static final String INVENTORY_RESERVATION_REQUESTED = "INVENTORY_RESERVATION_REQUESTED";
    public static final String INVENTORY_RESERVED = "INVENTORY_RESERVED";
    public static final String INVENTORY_RESERVATION_FAILED = "INVENTORY_RESERVATION_FAILED";
    public static final String INVENTORY_RESERVATION_EXPIRED = "INVENTORY_RESERVATION_EXPIRED";
    public static final String INVENTORY_RELEASED = "INVENTORY_RELEASED";
    public static final String INVENTORY_CONFIRMATION_REQUESTED = "INVENTORY_CONFIRMATION_REQUESTED";
    public static final String INVENTORY_CONFIRMED = "INVENTORY_CONFIRMED";
    public static final String INVENTORY_RELEASE_REQUESTED = "INVENTORY_RELEASE_REQUESTED";
    public static final String SHIPMENT_CREATION_REQUESTED = "SHIPMENT_CREATION_REQUESTED";
}
