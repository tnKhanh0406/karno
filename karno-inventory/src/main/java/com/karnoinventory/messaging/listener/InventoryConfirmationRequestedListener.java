package com.karnoinventory.messaging.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoinventory.messaging.handler.InventoryConfirmationRequestedHandler;
import com.karnoinventory.messaging.model.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryConfirmationRequestedListener {

    private static final String EXPECTED_EVENT_TYPE = "INVENTORY_CONFIRMATION_REQUESTED";
    private final ObjectMapper objectMapper;
    private final InventoryConfirmationRequestedHandler handler;

    @KafkaListener(topics = "${app.kafka.topics.inventory-confirmation-requested}")
    public void consume(ConsumerRecord<String, String> record) {
        log.info(
                "[STAGE 9] Confirmation received topic={}, offset={}, key={}",
                record.topic(),
                record.offset(),
                record.key()
        );

        EventEnvelope event = deserialize(record.value());

        if (!EXPECTED_EVENT_TYPE.equals(event.eventType())) {
            throw new IllegalArgumentException(
                    "Unexpected eventType: " + event.eventType()
            );
        }

        handler.handle(event);

        log.info(
                "[STAGE 9] Confirmation handled eventId={}",
                event.eventId()
        );
    }

    private EventEnvelope deserialize(String json) {
        try {
            return objectMapper.readValue(
                    json,
                    EventEnvelope.class
            );

        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(
                    "Invalid confirmation EventEnvelope",
                    ex
            );
        }
    }
}
