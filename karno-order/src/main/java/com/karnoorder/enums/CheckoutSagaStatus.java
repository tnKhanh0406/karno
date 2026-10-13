package com.karnoorder.enums;

public enum CheckoutSagaStatus {
    PROCESSING,
    INVENTORY_RESERVED,
    ORDER_COMMITTED,
    COMPENSATING,
    COMPLETED,
    FAILED
}