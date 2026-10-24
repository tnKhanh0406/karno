package com.karnofleet.service.impl;

import com.karnofleet.client.ShippingClient;
import com.karnofleet.client.UserClient;
import com.karnofleet.client.dto.PickupShipmentSnapshot;
import com.karnofleet.client.dto.ShopPickupContext;
import com.karnofleet.dto.request.CreatePickupRequest;
import com.karnofleet.enums.PickupRequestStatus;
import com.karnofleet.repository.PickupRequestRepo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PickupRequestServiceImplTest {
    @Test
    void createClaimsShipmentsAndPersistsOpenRequest() {
        PickupRequestRepo repo = mock(PickupRequestRepo.class);
        UserClient user = mock(UserClient.class);
        ShippingClient shipping = mock(ShippingClient.class);
        PickupRequestServiceImpl service = new PickupRequestServiceImpl(repo, user, shipping);
        UUID shopId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        when(user.getPickupContext(shopId)).thenReturn(new ShopPickupContext(
                shopId, sellerId, hubId, zoneId, "Shop", "0900", "Address",
                new BigDecimal("21.0"), new BigDecimal("105.0")));
        when(shipping.claim(any(), any())).thenReturn(List.of(new PickupShipmentSnapshot(
                shipmentId, UUID.randomUUID(), shopId, "TRACK", 1500,
                new BigDecimal("0.02"), "Shop", "0900", "Address",
                new BigDecimal("21.0"), new BigDecimal("105.0"))));
        when(repo.insert(any())).thenReturn(1);
        when(repo.insertShipment(any(), eq(shipmentId))).thenReturn(1);

        var result = service.create(new CreatePickupRequest(
                shopId, sellerId, List.of(shipmentId), LocalTime.of(8, 0),
                LocalTime.of(12, 0), 300));

        assertEquals(PickupRequestStatus.OPEN, result.status());
        assertEquals(new BigDecimal("1.500"), result.totalWeight());
        assertEquals(List.of(shipmentId), result.shipmentIds());
    }
}
