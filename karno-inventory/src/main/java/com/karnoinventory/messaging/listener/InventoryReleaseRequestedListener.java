package com.karnoinventory.messaging.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoinventory.messaging.handler.InventoryReleaseRequestedHandler;
import com.karnoinventory.messaging.model.EventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryReleaseRequestedListener {
    private final ObjectMapper objectMapper;
    private final InventoryReleaseRequestedHandler handler;

    @KafkaListener(topics = "${app.kafka.topics.inventory-release-requested}")
    public void consume(String message) {
        try {
            handler.handle(objectMapper.readValue(message, EventEnvelope.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid inventory release EventEnvelope", ex);
        }
    }
}
