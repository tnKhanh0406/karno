package com.karnoinventory.messaging.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoinventory.entity.OutboxEvent;
import com.karnoinventory.messaging.model.EventEnvelope;
import com.karnoinventory.repository.OutboxEventRepo;
import com.karnoinventory.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    private final OutboxEventRepo outboxEventRepo;
    private final OutboxService outboxStateService;
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
        List<OutboxEvent> events = outboxEventRepo.findPending(batchSize);
        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            String message = buildMessage(event);
            String key = event.getAggregateId().toString();

            kafkaTemplate
                    .send(
                            event.getTopic(),
                            key,
                            message
                    )
                    .get(
                            sendTimeoutMs,
                            TimeUnit.MILLISECONDS
                    );

            outboxStateService.markPublished(event.getId());

            log.info("Published outbox event: {}", event.getId());

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            handleFailure(event, ex);
        } catch (Exception ex) {
            handleFailure(event, ex);
        }
    }

    private String buildMessage(OutboxEvent event) throws JsonProcessingException {
        JsonNode data = objectMapper.readTree(event.getPayload());

        EventEnvelope envelope =
                new EventEnvelope(
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        1,
                        event.getCreatedAt(),
                        data
                );

        return objectMapper.writeValueAsString(envelope);
    }

    private void handleFailure(OutboxEvent event, Exception exception) {
        String error = exception.getMessage();

        log.error(
                "Failed to publish outbox event: {}",
                event.getId(),
                exception
        );

        int nextRetry = event.getRetryCount() + 1;
        if (nextRetry >= maxRetries) {
            outboxStateService.markFailed(
                    event.getId(),
                    error
            );
            return;
        }

        outboxStateService.markRetry(
                event.getId(),
                error
        );
    }
}
