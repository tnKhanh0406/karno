package com.karnouser.service;

import com.karnouser.dto.response.ShopInformationResponse;
import com.karnouser.dto.response.ShopLocationResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ShopService {
    ShopInformationResponse getShopInformation(UUID shopId);
    List<ShopInformationResponse> getShopsByIds(List<UUID> shopIds);
    List<ShopLocationResponse> getPickupLocations(Set<UUID> shopIds);
}
