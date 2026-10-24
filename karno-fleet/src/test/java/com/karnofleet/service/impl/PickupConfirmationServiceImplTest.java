package com.karnofleet.service.impl;

import com.karnofleet.client.ShippingClient;
import com.karnofleet.entity.PickupRequest;
import com.karnofleet.entity.PickupSession;
import com.karnofleet.enums.PickupRequestStatus;
import com.karnofleet.repository.FirstMileSessionRepo;
import com.karnofleet.repository.PickupRequestRepo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PickupConfirmationServiceImplTest {
    @Test
    void assignedShipperCanConfirmPlanningRequest() {
        PickupRequestRepo repo = mock(PickupRequestRepo.class);
        FirstMileSessionRepo sessionRepo = mock(FirstMileSessionRepo.class);
        ShippingClient shipping = mock(ShippingClient.class);
        PickupConfirmationServiceImpl service =
                new PickupConfirmationServiceImpl(repo, sessionRepo, shipping);
        UUID requestId = UUID.randomUUID();
        UUID shipperId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        PickupRequest request = PickupRequest.builder().id(requestId)
                .assignedShipperId(shipperId).pickupSessionId(sessionId)
                .status(PickupRequestStatus.PLANNING).build();
        when(repo.findById(requestId)).thenReturn(request);
        when(repo.findShipmentIds(requestId)).thenReturn(List.of(shipmentId));
        when(sessionRepo.findActiveByShipper(shipperId)).thenReturn(
                PickupSession.builder().id(sessionId).shipperId(shipperId).status("ACTIVE").build());
        when(repo.markPickedUp(requestId, shipperId, sessionId)).thenReturn(1);
        var result = service.confirm(requestId, shipperId);

        assertEquals(PickupRequestStatus.PICKED_UP, result.status());
        assertFalse(result.alreadyPickedUp());
        verify(shipping).confirmPickup(eq(requestId), anyMap());
    }
}
