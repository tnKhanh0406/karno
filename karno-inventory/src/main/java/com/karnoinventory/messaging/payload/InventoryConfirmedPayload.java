package com.karnoinventory.messaging.payload;

import java.util.UUID;

public record InventoryConfirmedPayload(
        UUID checkoutId
) {}