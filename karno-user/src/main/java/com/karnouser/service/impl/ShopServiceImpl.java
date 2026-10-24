package com.karnouser.service.impl;

import com.karnouser.dto.response.ShopInformationResponse;
import com.karnouser.dto.response.ShopPickupContextResponse;
import com.karnouser.dto.response.ShopLocationResponse;
import com.karnouser.exception.NotFoundException;
import com.karnouser.repository.ShopRepo;
import com.karnouser.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {
    private final ShopRepo shopRepo;

    @Override
    public ShopInformationResponse getShopInformation(UUID shopId) {
        ShopInformationResponse shop = shopRepo.findShopInformationById(shopId);
        if (shop == null) {
            throw new NotFoundException("Shop not found");
        }
        return shop;
    }

    @Override
    public List<ShopInformationResponse> getShopsByIds(List<UUID> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return List.of();
        }
        return shopRepo.findShopInformationByIds(shopIds);
    }

    @Override
    public List<ShopLocationResponse> getPickupLocations(Set<UUID> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return List.of();
        }
        return shopRepo.findShopLocationsByIds(shopIds);
    }

    @Override
    public ShopPickupContextResponse getPickupContext(UUID shopId) {
        ShopPickupContextResponse context = shopRepo.findPickupContextById(shopId);
        if (context == null) {
            throw new NotFoundException("Shop not found: " + shopId);
        }
        return context;
    }
}
