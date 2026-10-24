package com.karnoshipping.service.impl;

import com.karnoshipping.dto.request.PickupShipmentAssignmentRequest;
import com.karnoshipping.entity.Shipment;
import com.karnoshipping.enums.ShipmentStatus;
import com.karnoshipping.repository.ShipmentRepo;
import com.karnoshipping.repository.ShipmentStatusHistoryRepo;
import com.karnoshipping.service.OutboxService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PickupShipmentAssignmentServiceImplTest {
    @Test
    void claimMovesEveryShipmentToPickupRequested() {
        ShipmentRepo repo = mock(ShipmentRepo.class);
        ShipmentStatusHistoryRepo history = mock(ShipmentStatusHistoryRepo.class);
        OutboxService outbox = mock(OutboxService.class);
        PickupShipmentAssignmentServiceImpl service =
                new PickupShipmentAssignmentServiceImpl(repo, history, outbox);
        UUID requestId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        Shipment shipment = Shipment.builder().id(shipmentId).orderId(UUID.randomUUID())
                .shopId(shopId).status(ShipmentStatus.READY_FOR_PICKUP)
                .totalWeightGram(1000).totalVolumeM3(new BigDecimal("0.01")).build();
        when(repo.findByIdsForUpdate(List.of(shipmentId))).thenReturn(List.of(shipment));
        when(repo.assignToPickupRequest(requestId, shopId, List.of(shipmentId))).thenReturn(1);
        when(history.insertTransition(any(), eq(shipmentId), anyString(), anyString(), anyString()))
                .thenReturn(1);

        var result = service.claim(requestId,
                new PickupShipmentAssignmentRequest(shopId, List.of(shipmentId)));

        assertEquals(List.of(shipmentId), result.stream().map(s -> s.shipmentId()).toList());
        verify(outbox).enqueue(eq("PICKUP_REQUEST"), eq(requestId),
                eq("SHIPMENT_ADDED_TO_PICKUP_REQUEST"),
                eq("shipping.shipment.added-to-pickup-request"), any());
    }
}
