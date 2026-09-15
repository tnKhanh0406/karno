package com.karnoorder.service.impl;

import com.karnoorder.client.KarnoInventoryClient;
import com.karnoorder.client.KarnoProductClient;
import com.karnoorder.client.KarnoUserClient;
import com.karnoorder.dto.request.UUIDListRequest;
import com.karnoorder.dto.response.*;
import com.karnoorder.repository.CartItemRepo;
import com.karnoorder.repository.CartRepo;
import com.karnoorder.repository.projection.CartItemProjection;
import com.karnoorder.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepo cartRepo;
    private final CartItemRepo cartItemRepo;
    private final KarnoUserClient karnoUserClient;
    private final KarnoProductClient karnoProductClient;
    private final KarnoInventoryClient karnoInventoryClient;

    @Override
    public CartResponse getCart(UUID userId) {
        UUID cartId = cartRepo.findCartIdByUserId(userId);
        int totalItems = cartItemRepo.countCartItemsByCartId(cartId);
        List<CartItemProjection> cartItems = cartItemRepo.getCartItemsByCartId(cartId);
        List<UUID> variantIds = cartItems.stream()
                .map(CartItemProjection::getVariantId)
                .distinct()
                .toList();
        List<VariantCartResponse> variantCarts = karnoProductClient.getVariantCartInfo(new UUIDListRequest(variantIds));
        Map<UUID, List<VariantCartResponse>> variantCartsByShopId = variantCarts.stream()
                .collect(Collectors.groupingBy(VariantCartResponse::shopId));
        List<VariantInventoryResponse> variantInventories = karnoInventoryClient.getAvailabilityByVariantIds(new UUIDListRequest(variantIds));

        List<UUID> shopIds = variantCarts.stream()
                .map(VariantCartResponse::shopId)
                .distinct()
                .toList();
        List<ShopInformationResponse> shopInformation = karnoUserClient.getShopsByIds(new UUIDListRequest(shopIds));

        List<CartShopResponse> shops = new ArrayList<>();
        return new CartResponse(
                cartId,
                totalItems,
                shops
        );
    }
}
