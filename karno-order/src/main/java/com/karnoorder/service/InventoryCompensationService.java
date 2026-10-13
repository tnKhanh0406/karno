package com.karnoorder.service;

import java.util.UUID;

public interface InventoryCompensationService {
    void startCompensation(UUID checkoutId, String failureCode, String failureMessage);
}
