package com.karnofleet.service;

import com.karnofleet.component.LastMileOrderSelector;
import com.karnofleet.dto.request.LastMileOrder;
import com.karnofleet.dto.response.FirstMileContext;
import com.karnofleet.dto.response.LastMileLoadResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LastMileLoadingService {
    private final FirstMileContextService contextService;
    private final LastMileClusteringService clusteringService;
    private final LastMileOrderSelector selector;

    public LastMileLoadResponse select(
            UUID shipperUserId,
            List<LastMileOrder> orders,
            double maxDiameterMeters
    ) {
        FirstMileContext context = contextService.getContext(shipperUserId, java.time.LocalDateTime.now());
        var clusters = clusteringService.cluster(orders, maxDiameterMeters);
        return selector.select(
                clusters,
                context.vehicle().getCapacityWeight(),
                context.vehicle().getCapacityVolume());
    }

    public LastMileLoadResponse selectDemo(
            UUID shipperUserId,
            double maxDiameterMeters
    ) {
        FirstMileContext context = contextService.getContext(shipperUserId, java.time.LocalDateTime.now());
        var clusters = clusteringService.demo(maxDiameterMeters);
        return selector.select(
                clusters,
                context.vehicle().getCapacityWeight(),
                context.vehicle().getCapacityVolume());
    }

    public LastMileLoadResponse selectWithCapacity(
            List<LastMileOrder> orders,
            double maxDiameterMeters,
            BigDecimal capacityWeight,
            BigDecimal capacityVolume
    ) {
        var clusters = clusteringService.cluster(orders, maxDiameterMeters);
        return selector.select(clusters, capacityWeight, capacityVolume);
    }
}
