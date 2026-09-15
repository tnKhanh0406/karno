package com.karnouser.controller.internal;

import com.karnouser.dto.response.ShopInformationResponse;
import com.karnouser.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/shops")
@RequiredArgsConstructor
public class ShopInternalController {
    private final ShopService shopService;

    @GetMapping("/{shopId}")
    public ShopInformationResponse getShopInformation(@PathVariable UUID shopId) {
        return shopService.getShopInformation(shopId);
    }
}
