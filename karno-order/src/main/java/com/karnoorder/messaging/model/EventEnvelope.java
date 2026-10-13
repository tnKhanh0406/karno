package com.karnoorder.messaging.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        Integer schemaVersion,
        Instant occurredAt,
        JsonNode data
) {
}
