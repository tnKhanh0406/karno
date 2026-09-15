package com.karnouser.service;

import com.karnouser.dto.response.ShopInformationResponse;

import java.util.List;
import java.util.UUID;

public interface ShopService {
    ShopInformationResponse getShopInformation(UUID shopId);
    List<ShopInformationResponse> getShopsByIds(List<UUID> shopIds);
}
