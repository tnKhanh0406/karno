package com.karnohub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.karnohub.entity.Hub;
import com.karnohub.entity.HubConnection;
import com.karnohub.enums.HubConnectionStatus;
import com.karnohub.enums.HubStatus;
import com.karnohub.repository.HubConnectionRepo;
import com.karnohub.repository.HubRepo;
import com.karnohub.service.impl.HubServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HubServiceTest {

    @Mock
    private HubRepo hubRepo;

    @Mock
    private HubConnectionRepo hubConnectionRepo;

    @InjectMocks
    private HubServiceImpl hubService;

    private Hub hubHn;
    private Hub hubHcm;
    private HubConnection connHnHcm;

    @BeforeEach
    void setUp() {
        hubHn = Hub.builder()
                .id(UUID.fromString("c1000000-0000-0000-0000-000000000001"))
                .hubCode("HN01")
                .name("Hà Nội Hub")
                .minTransferMinutes(120)
                .status(HubStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        hubHcm = Hub.builder()
                .id(UUID.fromString("c1000000-0000-0000-0000-000000000002"))
                .hubCode("HCM01")
                .name("Hồ Chí Minh Hub")
                .minTransferMinutes(120)
                .status(HubStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        connHnHcm = HubConnection.builder()
                .id(UUID.fromString("c2000000-0000-0000-0000-000000000001"))
                .fromHubId(hubHn.getId())
                .toHubId(hubHcm.getId())
                .status(HubConnectionStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetAllActiveHubs() {
        when(hubRepo.findByStatus(HubStatus.ACTIVE.name())).thenReturn(List.of(hubHn, hubHcm));

        List<Hub> hubs = hubService.getAllActiveHubs();

        assertEquals(2, hubs.size());
        assertEquals("HN01", hubs.get(0).getHubCode());
        assertEquals("HCM01", hubs.get(1).getHubCode());
    }

    @Test
    void testGetHubByCode() {
        when(hubRepo.findByHubCode("HN01")).thenReturn(Optional.of(hubHn));

        Optional<Hub> result = hubService.getHubByCode("HN01");

        assertTrue(result.isPresent());
        assertEquals("Hà Nội Hub", result.get().getName());
        assertEquals(120, result.get().getMinTransferMinutes());
    }

    @Test
    void testGetConnectionsFromHub() {
        when(hubConnectionRepo.findByFromHubId(hubHn.getId())).thenReturn(List.of(connHnHcm));

        List<HubConnection> connections = hubService.getConnectionsFromHub(hubHn.getId());

        assertEquals(1, connections.size());
        assertEquals(hubHcm.getId(), connections.get(0).getToHubId());
    }
}
