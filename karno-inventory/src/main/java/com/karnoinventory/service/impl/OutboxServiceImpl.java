package com.karnoinventory.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoinventory.entity.OutboxEvent;
import com.karnoinventory.enums.OutboxEventStatus;
import com.karnoinventory.exception.OutboxSerializationException;
import com.karnoinventory.repository.OutboxEventRepo;
import com.karnoinventory.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {
    private final OutboxEventRepo outboxEventRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public UUID enqueueEvent(String aggregateType, UUID aggregateId, String eventType, String topic, Object payload) {
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new OutboxSerializationException(
                    "Failed to serialize outbox payload",
                    ex
            );
        }
        UUID eventId = UUID.randomUUID();
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(eventId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .topic(topic)
                .payload(jsonPayload)
                .status(OutboxEventStatus.NEW)
                .retryCount(0)
                .build();
        int inserted = outboxEventRepo.insert(outboxEvent);
        if (inserted != 1) {
            throw new IllegalStateException("Failed to insert outbox event");
        }
        return eventId;
    }
}
