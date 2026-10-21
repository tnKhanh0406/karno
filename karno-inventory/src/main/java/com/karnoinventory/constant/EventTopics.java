package com.karnoinventory.constant;

public class EventTopics {
    private EventTopics() {}
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_RESERVATION_FAILED = "inventory.reservation.failed";
    public static final String INVENTORY_RELEASED = "inventory.released";
    public static final String INVENTORY_RESERVATION_EXPIRED = "inventory.reservation.expired";
    public static final String INVENTORY_CONFIRMED = "inventory.confirmed";
}
