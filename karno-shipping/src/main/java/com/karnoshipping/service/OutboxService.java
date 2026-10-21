package com.karnoshipping.service;

import java.util.UUID;

public interface OutboxService {
    UUID enqueue(String aggregateType, UUID aggregateId, String eventType,
                 String topic, Object payload);
}
