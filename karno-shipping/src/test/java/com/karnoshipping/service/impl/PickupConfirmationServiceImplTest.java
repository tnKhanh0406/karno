package com.karnoshipping.service.impl;

import com.karnoshipping.entity.Shipment;
import com.karnoshipping.enums.ShipmentStatus;
import com.karnoshipping.repository.ShipmentRepo;
import com.karnoshipping.repository.ShipmentStatusHistoryRepo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PickupConfirmationServiceImplTest {
    @Test
    void confirmMovesTheWholeManifestToPickedUp() {
        ShipmentRepo repo = mock(ShipmentRepo.class);
        ShipmentStatusHistoryRepo history = mock(ShipmentStatusHistoryRepo.class);
        PickupConfirmationServiceImpl service = new PickupConfirmationServiceImpl(repo, history);
        UUID requestId = UUID.randomUUID();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        List<UUID> ids = List.of(firstId, secondId);
        when(repo.findByPickupRequestIdForUpdate(requestId)).thenReturn(List.of(
                Shipment.builder().id(firstId).pickupRequestId(requestId)
                        .status(ShipmentStatus.PICKUP_PLANNED).build(),
                Shipment.builder().id(secondId).pickupRequestId(requestId)
                        .status(ShipmentStatus.PICKUP_PLANNED).build()));
        when(repo.markPickedUp(requestId, ids)).thenReturn(2);
        when(history.insertTransition(any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(1);

        service.confirm(requestId, ids);

        verify(repo).markPickedUp(requestId, ids);
        verify(history, times(2)).insertTransition(
                any(), any(), eq("PICKUP_PLANNED"), eq("PICKED_UP"), anyString());
    }

    @Test
    void completeSessionMovesPickedToHubAndReleasesUnpicked() {
        ShipmentRepo repo = mock(ShipmentRepo.class);
        ShipmentStatusHistoryRepo history = mock(ShipmentStatusHistoryRepo.class);
        PickupConfirmationServiceImpl service = new PickupConfirmationServiceImpl(repo, history);
        UUID pickedRequest = UUID.randomUUID();
        UUID unpickedRequest = UUID.randomUUID();
        Shipment picked = Shipment.builder().id(UUID.randomUUID()).pickupRequestId(pickedRequest)
                .status(ShipmentStatus.PICKED_UP).build();
        Shipment unpicked = Shipment.builder().id(UUID.randomUUID()).pickupRequestId(unpickedRequest)
                .status(ShipmentStatus.PICKUP_PLANNED).build();
        when(repo.findByPickupRequestIdsForUpdate(List.of(pickedRequest)))
                .thenReturn(List.of(picked));
        when(repo.findByPickupRequestIdsForUpdate(List.of(unpickedRequest)))
                .thenReturn(List.of(unpicked));
        when(repo.markAtOriginHub(List.of(pickedRequest))).thenReturn(1);
        when(repo.revertPickupPlanned(List.of(unpickedRequest))).thenReturn(1);
        when(history.insertTransition(any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(1);

        service.completeSession(List.of(pickedRequest), List.of(unpickedRequest));

        verify(repo).markAtOriginHub(List.of(pickedRequest));
        verify(repo).revertPickupPlanned(List.of(unpickedRequest));
        verify(history).insertTransition(any(), eq(picked.getId()), eq("PICKED_UP"),
                eq("AT_ORIGIN_HUB"), anyString());
        verify(history).insertTransition(any(), eq(unpicked.getId()), eq("PICKUP_PLANNED"),
                eq("PICKUP_REQUESTED"), anyString());
    }

    @Test
    void completeSessionIsIdempotentWhenShippingAlreadyConverged() {
        ShipmentRepo repo = mock(ShipmentRepo.class);
        ShipmentStatusHistoryRepo history = mock(ShipmentStatusHistoryRepo.class);
        PickupConfirmationServiceImpl service = new PickupConfirmationServiceImpl(repo, history);
        UUID requestId = UUID.randomUUID();
        when(repo.findByPickupRequestIdsForUpdate(List.of(requestId))).thenReturn(List.of(
                Shipment.builder().id(UUID.randomUUID()).pickupRequestId(requestId)
                        .status(ShipmentStatus.AT_ORIGIN_HUB).build()));

        service.completeSession(List.of(requestId), List.of());

        verify(repo).markAtOriginHub(List.of(requestId));
        verifyNoInteractions(history);
    }
}
