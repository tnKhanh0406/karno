package com.karnofleet.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnofleet.client.ShippingClient;
import com.karnofleet.dto.request.FirstMilePlanRequest;
import com.karnofleet.entity.PickupRequest;
import com.karnofleet.entity.PickupSession;
import com.karnofleet.enums.PickupRequestStatus;
import com.karnofleet.repository.FirstMileSessionRepo;
import com.karnofleet.repository.PickupRequestRepo;
import com.karnofleet.repository.VehicleRepo;
import com.karnofleet.service.FirstMilePlanningService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class PickupSessionServiceImplTest {
    private final FirstMileSessionRepo sessionRepo = mock(FirstMileSessionRepo.class);
    private final VehicleRepo vehicleRepo = mock(VehicleRepo.class);
    private final PickupRequestRepo requestRepo = mock(PickupRequestRepo.class);
    private final FirstMilePlanningService planning = mock(FirstMilePlanningService.class);
    private final ShippingClient shipping = mock(ShippingClient.class);
    private final PickupSessionServiceImpl service = new PickupSessionServiceImpl(
            sessionRepo, vehicleRepo, requestRepo, planning, shipping, new ObjectMapper());

    @Test
    void startReturnsCurrentSessionInsteadOfCreatingAnother() {
        UUID shipperId = UUID.randomUUID();
        PickupSession current = PickupSession.builder().id(UUID.randomUUID()).shipperId(shipperId)
                .vehicleId(UUID.randomUUID()).status("ACTIVE").build();
        when(sessionRepo.findActiveByShipper(shipperId)).thenReturn(current);
        when(requestRepo.findBySession(current.getId())).thenReturn(List.of());

        var response = service.start(shipperId, request(current.getVehicleId()));

        assertEquals(current.getId(), response.sessionId());
        verifyNoInteractions(planning);
        verify(sessionRepo, never()).insert(any());
    }

    @Test
    void cancelIsRejectedAfterAnyPickupWasConfirmed() {
        UUID shipperId = UUID.randomUUID();
        PickupSession session = activeSession(shipperId);
        when(sessionRepo.findByIdForUpdate(session.getId())).thenReturn(session);
        when(requestRepo.countPickedUpBySession(session.getId())).thenReturn(1);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.cancel(shipperId, session.getId()));

        assertEquals(409, error.getStatusCode().value());
        verify(shipping, never()).unplan(anyMap());
        verify(vehicleRepo, never()).release(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void completePartitionsPickedAndUnpickedAndReleasesResources() {
        UUID shipperId = UUID.randomUUID();
        PickupSession session = activeSession(shipperId);
        UUID pickedId = UUID.randomUUID();
        UUID unpickedId = UUID.randomUUID();
        List<PickupRequest> requests = List.of(
                PickupRequest.builder().id(pickedId).status(PickupRequestStatus.PICKED_UP).build(),
                PickupRequest.builder().id(unpickedId).status(PickupRequestStatus.PLANNING).build());
        when(sessionRepo.findByIdForUpdate(session.getId())).thenReturn(session);
        when(sessionRepo.markCompleting(session.getId(), shipperId)).thenReturn(1);
        when(requestRepo.findBySession(session.getId())).thenReturn(requests);
        when(vehicleRepo.release(session.getVehicleId())).thenReturn(1);
        when(sessionRepo.markCompleted(session.getId())).thenReturn(1);

        var response = service.complete(shipperId, session.getId());

        assertEquals("COMPLETED", response.status());
        var payload = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(shipping).completeSession(payload.capture());
        assertEquals(List.of(pickedId), payload.getValue().get("pickedUpPickupRequestIds"));
        assertEquals(List.of(unpickedId), payload.getValue().get("unpickedPickupRequestIds"));
        verify(requestRepo).markPickedUpAtOriginHub(session.getId());
        verify(requestRepo).revertSession(session.getId());
        verify(vehicleRepo).release(session.getVehicleId());
    }

    private PickupSession activeSession(UUID shipperId) {
        return PickupSession.builder().id(UUID.randomUUID()).shipperId(shipperId)
                .vehicleId(UUID.randomUUID()).hubId(UUID.randomUUID()).zoneId(UUID.randomUUID())
                .status("ACTIVE").build();
    }

    private FirstMilePlanRequest request(UUID vehicleId) {
        return new FirstMilePlanRequest(vehicleId, BigDecimal.ONE, BigDecimal.ONE,
                LocalDateTime.now(), LocalDateTime.now().plusHours(4));
    }
}
