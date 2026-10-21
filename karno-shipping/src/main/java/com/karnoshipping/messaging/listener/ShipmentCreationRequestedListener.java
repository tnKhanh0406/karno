package com.karnoshipping.messaging.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoshipping.messaging.handler.ShipmentCreationRequestedHandler;
import com.karnoshipping.messaging.model.EventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShipmentCreationRequestedListener {
    private final ObjectMapper objectMapper;
    private final ShipmentCreationRequestedHandler handler;

    @KafkaListener(topics = "${app.kafka.topics.shipment-creation-requested}")
    public void consume(String message) {
        try {
            handler.handle(objectMapper.readValue(message, EventEnvelope.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid shipment EventEnvelope", ex);
        }
    }
}
