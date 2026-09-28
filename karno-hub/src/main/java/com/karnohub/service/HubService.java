package com.karnohub.service;

import com.karnohub.entity.Hub;
import com.karnohub.entity.HubConnection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubService {
    List<Hub> getAllActiveHubs();

    Optional<Hub> getHubById(UUID id);

    Optional<Hub> getHubByCode(String hubCode);

    List<HubConnection> getConnectionsFromHub(UUID fromHubId);

    List<HubConnection> getAllActiveConnections();
}
