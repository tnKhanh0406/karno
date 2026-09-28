package com.karnohub.service.impl;

import com.karnohub.entity.Hub;
import com.karnohub.entity.HubConnection;
import com.karnohub.enums.HubConnectionStatus;
import com.karnohub.enums.HubStatus;
import com.karnohub.repository.HubConnectionRepo;
import com.karnohub.repository.HubRepo;
import com.karnohub.service.HubService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HubServiceImpl implements HubService {

    private final HubRepo hubRepo;
    private final HubConnectionRepo hubConnectionRepo;

    @Override
    public List<Hub> getAllActiveHubs() {
        return hubRepo.findByStatus(HubStatus.ACTIVE.name());
    }

    @Override
    public Optional<Hub> getHubById(UUID id) {
        return hubRepo.findById(id);
    }

    @Override
    public Optional<Hub> getHubByCode(String hubCode) {
        return hubRepo.findByHubCode(hubCode);
    }

    @Override
    public List<HubConnection> getConnectionsFromHub(UUID fromHubId) {
        return hubConnectionRepo.findByFromHubId(fromHubId);
    }

    @Override
    public List<HubConnection> getAllActiveConnections() {
        return hubConnectionRepo.findByStatus(HubConnectionStatus.ACTIVE.name());
    }
}
