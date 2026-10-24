package com.karnoshipping.messaging.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoshipping.entity.Shipment;
import com.karnoshipping.enums.ShipmentStatus;
import com.karnoshipping.messaging.model.EventEnvelope;
import com.karnoshipping.messaging.payload.OrderPackedPayload;
import com.karnoshipping.repository.ProcessedEventRepo;
import com.karnoshipping.repository.ShipmentRepo;
import com.karnoshipping.repository.ShipmentStatusHistoryRepo;
import com.karnoshipping.service.OutboxService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderPackedHandlerTest {

    @Test
    void orderPackedChangesCreatedShipmentToReadyForPickup() {
        ProcessedEventRepo processedEventRepo = mock(ProcessedEventRepo.class);
        ShipmentRepo shipmentRepo = mock(ShipmentRepo.class);
        ShipmentStatusHistoryRepo historyRepo = mock(ShipmentStatusHistoryRepo.class);
        OutboxService outboxService = mock(OutboxService.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        OrderPackedHandler handler = new OrderPackedHandler(
                processedEventRepo, shipmentRepo, historyRepo, outboxService, objectMapper);

        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Instant packedAt = Instant.now();
        OrderPackedPayload payload = new OrderPackedPayload(orderId, shopId, sellerId, packedAt);
        EventEnvelope event = new EventEnvelope(
                eventId, "ORDER_PACKED", "ORDER", orderId, 1, packedAt,
                objectMapper.valueToTree(payload));
        Shipment shipment = Shipment.builder().id(shipmentId).orderId(orderId)
                .shopId(shopId).status(ShipmentStatus.CREATED).build();

        when(processedEventRepo.insertIfAbsent(eventId, "order-packed-handler", "ORDER_PACKED"))
                .thenReturn(1);
        when(shipmentRepo.findByOrderId(orderId)).thenReturn(shipment);
        when(shipmentRepo.markReadyForPickup(orderId, shopId)).thenReturn(1);
        when(historyRepo.insertTransition(any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(1);

        handler.handle(event);

        verify(shipmentRepo).markReadyForPickup(orderId, shopId);
        verify(outboxService).enqueue(
                org.mockito.ArgumentMatchers.eq("SHIPMENT"),
                org.mockito.ArgumentMatchers.eq(shipmentId),
                org.mockito.ArgumentMatchers.eq("SHIPMENT_READY_FOR_PICKUP"),
                org.mockito.ArgumentMatchers.eq("shipping.shipment.ready-for-pickup"), any());
    }
}
