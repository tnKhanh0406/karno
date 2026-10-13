package com.karnoinventory.service;

import java.util.UUID;

public interface OutboxService {
    UUID enqueueEvent(String aggregateType, UUID aggregateId, String eventType, String topic, Object payload);
    void markPublished(UUID eventId);
    void markRetry(UUID eventId, String error);
    void markFailed(UUID eventId, String error);
}
