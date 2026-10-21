package com.karnoshipping.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoshipping.entity.OutboxEvent;
import com.karnoshipping.enums.OutboxEventStatus;
import com.karnoshipping.repository.OutboxEventRepo;
import com.karnoshipping.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {
    private final OutboxEventRepo outboxEventRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public UUID enqueue(String aggregateType, UUID aggregateId, String eventType,
                        String topic, Object payload) {
        try {
            UUID id = UUID.randomUUID();
            OutboxEvent event = OutboxEvent.builder()
                    .id(id).aggregateType(aggregateType).aggregateId(aggregateId)
                    .eventType(eventType).topic(topic)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status(OutboxEventStatus.NEW).retryCount(0).build();
            if (outboxEventRepo.insert(event) != 1) {
                throw new IllegalStateException("Cannot insert shipping outbox event");
            }
            return id;
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Cannot serialize shipping outbox payload", ex);
        }
    }
}
