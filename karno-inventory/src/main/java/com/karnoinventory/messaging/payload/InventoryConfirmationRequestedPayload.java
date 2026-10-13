package com.karnoinventory.messaging.payload;

import java.util.UUID;

public record InventoryConfirmationRequestedPayload(
        UUID checkoutId
) {}