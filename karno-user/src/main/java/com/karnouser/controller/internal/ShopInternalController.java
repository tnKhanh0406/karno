package com.karnouser.controller.internal;

import com.karnouser.dto.request.UUIDListRequest;
import com.karnouser.dto.response.ShopInformationResponse;
import com.karnouser.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @PostMapping("/batch")
    public List<ShopInformationResponse> getShopsByIds(@RequestBody UUIDListRequest request) {
        return shopService.getShopsByIds(request.UUIDs());
    }
}
