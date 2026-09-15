package com.karnouser.service;

import com.karnouser.dto.response.ShopInformationResponse;

import java.util.UUID;

public interface ShopService {
    ShopInformationResponse getShopInformation(UUID shopId);
}
