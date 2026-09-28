package com.karnoinventory.service;

import java.util.UUID;

public interface OutboxService {
    UUID enqueueEvent(String aggregateType, UUID aggregateId, String eventType, String topic, Object payload);
}
