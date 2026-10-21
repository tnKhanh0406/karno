package com.karnoshipping.messaging.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoshipping.entity.OutboxEvent;
import com.karnoshipping.messaging.model.EventEnvelope;
import com.karnoshipping.repository.OutboxEventRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    private final OutboxEventRepo outboxEventRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${outbox.publisher.batch-size:50}")
    private int batchSize;
    @Value("${outbox.publisher.send-timeout-ms:5000}")
    private long sendTimeoutMs;
    @Value("${outbox.publisher.max-retries:10}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:1000}")
    public void publishPendingEvents() {
        for (OutboxEvent event : outboxEventRepo.findPending(batchSize)) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            JsonNode data = objectMapper.readTree(event.getPayload());
            EventEnvelope envelope = new EventEnvelope(
                    event.getId(), event.getEventType(), event.getAggregateType(),
                    event.getAggregateId(), 1, event.getCreatedAt(), data);
            kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(),
                    objectMapper.writeValueAsString(envelope))
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            outboxEventRepo.markPublished(event.getId(), Instant.now());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            fail(event, ex);
        } catch (Exception ex) {
            fail(event, ex);
        }
    }

    private void fail(OutboxEvent event, Exception ex) {
        log.error("Cannot publish shipping outbox eventId={}", event.getId(), ex);
        if (event.getRetryCount() + 1 >= maxRetries) {
            outboxEventRepo.markFailed(event.getId());
        } else {
            outboxEventRepo.markRetry(event.getId());
        }
    }
}
