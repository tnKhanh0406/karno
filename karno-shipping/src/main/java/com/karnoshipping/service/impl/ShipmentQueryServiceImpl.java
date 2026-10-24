package com.karnoshipping.service.impl;

import com.karnoshipping.dto.response.ReadyShipmentResponse;
import com.karnoshipping.repository.ShipmentRepo;
import com.karnoshipping.service.ShipmentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentQueryServiceImpl implements ShipmentQueryService {
    private final ShipmentRepo shipmentRepo;

    @Override
    @Transactional(readOnly = true)
    public List<ReadyShipmentResponse> getReadyForPickup(UUID shopId) {
        return shipmentRepo.findReadyForPickupByShopId(shopId).stream()
                .map(ReadyShipmentResponse::from)
                .toList();
    }
}
