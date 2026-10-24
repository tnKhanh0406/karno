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
}
