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
import java.util.function.Function;
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
        String cartIdStr = cartRepo.findCartIdByUserId(userId);
        if (cartIdStr == null) {
            return new CartResponse(null, 0, new ArrayList<>());
        }
        UUID cartId = UUID.fromString(cartRepo.findCartIdByUserId(userId));

        List<CartItemProjection> cartItems = cartItemRepo.getCartItemsByCartId(cartId);

        if (cartItems.isEmpty()) {
            return new CartResponse(cartId, 0, List.of());
        }

        List<UUID> variantIds = cartItems.stream()
                .map(CartItemProjection::getVariantId)
                .distinct()
                .toList();

        List<VariantCartResponse> variants = karnoProductClient.getVariantCartInfo(new UUIDListRequest(variantIds));

        Map<UUID, VariantCartResponse> variantMap = variants.stream()
                        .collect(Collectors.toMap(
                                VariantCartResponse::id,
                                Function.identity()
                        ));

        List<VariantInventoryResponse> inventories = karnoInventoryClient.getAvailabilityByVariantIds(new UUIDListRequest(variantIds));

        Map<UUID, Integer> stockMap = inventories.stream()
                        .collect(Collectors.toMap(
                                VariantInventoryResponse::variantId,
                                VariantInventoryResponse::availableQuantity
                        ));

        List<UUID> shopIds = cartItems.stream()
                .map(CartItemProjection::getShopId)
                .distinct()
                .toList();

        List<ShopInformationResponse> shopInfos = karnoUserClient.getShopsByIds(new UUIDListRequest(shopIds));

        Map<UUID, ShopInformationResponse> shopMap = shopInfos.stream()
                        .collect(Collectors.toMap(
                                ShopInformationResponse::id,
                                Function.identity()
                        ));

        Map<UUID, List<CartItemProjection>> cartItemsByShop = cartItems.stream()
                        .collect(Collectors.groupingBy(
                                CartItemProjection::getShopId
                        ));

        List<CartShopResponse> shops = cartItemsByShop.entrySet()
                        .stream()
                        .map(entry -> buildCartShopResponse(
                                entry.getKey(),
                                entry.getValue(),
                                shopMap,
                                variantMap,
                                stockMap
                        ))
                        .toList();

        int totalItems = cartItemRepo.countCartItemsByCartId(cartId);

        return new CartResponse(
                cartId,
                totalItems,
                shops
        );
    }

    private CartShopResponse buildCartShopResponse(UUID shopId,
                                                   List<CartItemProjection> cartItems,
                                                   Map<UUID, ShopInformationResponse> shopMap,
                                                   Map<UUID, VariantCartResponse> variantMap,
                                                   Map<UUID, Integer> stockMap) {

        ShopInformationResponse shop = shopMap.get(shopId);
        List<CartItemResponse> items = cartItems.stream()
                .map(cartItem -> buildCartItemResponse(
                        cartItem,
                        variantMap,
                        stockMap
                ))
                .toList();
        return new CartShopResponse(
                shopId,
                shop != null ? shop.name() : null,
                shop != null ? shop.logoUrl() : null,
                items
        );
    }

    private CartItemResponse buildCartItemResponse(CartItemProjection cartItem,
                                                   Map<UUID, VariantCartResponse> variantMap,
                                                   Map<UUID, Integer> stockMap) {
        VariantCartResponse variant = variantMap.get(cartItem.getVariantId());
        if (variant == null) {
            return new CartItemResponse(
                    cartItem.getId(),
                    cartItem.getProductId(),
                    null,
                    null,
                    cartItem.getVariantId(),
                    null,
                    null,
                    null,
                    cartItem.getQuantity(),
                    0,
                    List.of(),
                    false,
                    "PRODUCT_NOT_FOUND"
            );
        }
        Integer availableStock = stockMap.getOrDefault(variant.id(), 0);
        String unavailableReason = getUnavailableReason(variant, availableStock, cartItem.getQuantity());
        boolean available = unavailableReason == null;

        return new CartItemResponse(
                cartItem.getId(),
                variant.productId(),
                variant.productName(),
                variant.thumbnailImg(),
                variant.id(),
                variant.name(),
                variant.primaryImg(),
                variant.price(),
                cartItem.getQuantity(),
                availableStock,
                variant.attributeValues(),
                available,
                unavailableReason
        );
    }

    private String getUnavailableReason(VariantCartResponse variant, Integer availableStock, Integer requestedQuantity) {
        if (!"ACTIVE".equalsIgnoreCase(variant.status())) {
            return "PRODUCT_UNAVAILABLE";
        }
        if (availableStock <= 0) {
            return "OUT_OF_STOCK";
        }
        if (availableStock < requestedQuantity) {
            return "INSUFFICIENT_STOCK";
        }
        return null;
    }
}
