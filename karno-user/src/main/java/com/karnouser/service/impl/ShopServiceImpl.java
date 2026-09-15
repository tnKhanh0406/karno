package com.karnouser.service.impl;

import com.karnouser.dto.response.ShopInformationResponse;
import com.karnouser.exception.NotFoundException;
import com.karnouser.repository.ShopRepo;
import com.karnouser.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
