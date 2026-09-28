package com.karnoinventory.messaging.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoinventory.handler.InventoryReservationRequestedHandler;
import com.karnoinventory.messaging.model.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationRequestedListener {

    private static final String EXPECTED_EVENT_TYPE = "INVENTORY_RESERVATION_REQUESTED";

    private final ObjectMapper objectMapper;
    private final InventoryReservationRequestedHandler handler;

    @KafkaListener(topics = "${app.kafka.topics.inventory-reservation-requested}")
    public void consume(ConsumerRecord<String, String> record) {
        log.info(
                "Received Kafka event. topic={}, partition={}, offset={}, key={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key()
        );

        EventEnvelope event = deserialize(record.value());
        if (!EXPECTED_EVENT_TYPE.equals(event.eventType())) {
            throw new IllegalArgumentException("Unexpected event type: " + event.eventType());
        }

        handler.handle(event);
    }


    private EventEnvelope deserialize(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    EventEnvelope.class
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid Kafka event payload", ex);
        }
    }
}