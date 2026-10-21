package com.karnoorder.component;

import com.karnoorder.client.KarnoProductClient;
import com.karnoorder.client.KarnoUserClient;
import com.karnoorder.dto.request.CreateOrderRequest;
import com.karnoorder.dto.request.CheckoutPreviewRequest;
import com.karnoorder.dto.request.ShippingSelectionRequest;
import com.karnoorder.dto.request.UUIDListRequest;
import com.karnoorder.dto.response.CheckoutAddressResponse;
import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.dto.response.ShopInformationResponse;
import com.karnoorder.dto.response.VariantCheckoutInfo;
import com.karnoorder.entity.CartItem;
import com.karnoorder.exception.CheckoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CheckoutDataLoader {
    private final com.karnoorder.repository.CartItemRepo cartItemRepo;
    private final KarnoProductClient productClient;
    private final KarnoUserClient userClient;

    public CheckoutData load(UUID customerId, CreateOrderRequest request) {
        return load(
                customerId,
                request.cartItemIds(),
                request.addressId(),
                request.shippingSelections(),
                true
        );
    }

    public CheckoutData loadPreview(UUID customerId, CheckoutPreviewRequest request) {
        List<CartItem> cartItems = loadCartItems(customerId, request.cartItemIds());
        Map<UUID, VariantCheckoutInfo> variants = loadVariantMap(cartItems);
        Map<UUID, List<CartItem>> itemsByShop = groupByShop(cartItems, variants);
        Map<UUID, ShippingSelectionRequest> selections = defaultShippingSelections(
                itemsByShop.keySet()
        );
        Map<UUID, ShopInformationResponse> shops = loadShopMap(itemsByShop.keySet());
        CheckoutAddressResponse address = userClient.getUserAddressById(request.addressId());
        CheckoutContext.Receiver receiver = buildReceiver(address);
        List<OrderSeed> seeds = buildOrderSeeds(
                itemsByShop,
                variants,
                shops,
                selections,
                false
        );

        return new CheckoutData(
                variants,
                itemsByShop,
                selections,
                shops,
                address,
                receiver,
                seeds
        );
    }

    private CheckoutData load(
            UUID customerId,
            Set<UUID> cartItemIds,
            UUID addressId,
            List<ShippingSelectionRequest> shippingSelections,
            boolean validateDisplayedValues
    ) {
        List<CartItem> cartItems = loadCartItems(customerId, cartItemIds);
        Map<UUID, VariantCheckoutInfo> variants = loadVariantMap(cartItems);
        Map<UUID, List<CartItem>> itemsByShop = groupByShop(cartItems, variants);
        Map<UUID, ShippingSelectionRequest> selections =
                mapShippingSelections(shippingSelections, itemsByShop.keySet(), validateDisplayedValues);
        Map<UUID, ShopInformationResponse> shops = loadShopMap(itemsByShop.keySet());
        CheckoutAddressResponse address = userClient.getUserAddressById(addressId);
        CheckoutContext.Receiver receiver = buildReceiver(address);
        List<OrderSeed> seeds = buildOrderSeeds(
                itemsByShop,
                variants,
                shops,
                selections,
                validateDisplayedValues
        );

        return new CheckoutData(
                variants,
                itemsByShop,
                selections,
                shops,
                address,
                receiver,
                seeds
        );
    }

    private List<CartItem> loadCartItems(UUID customerId, Set<UUID> cartItemIds) {
        List<CartItem> items = cartItemRepo.findSelectedByCustomer(customerId, cartItemIds);

        if (items == null || items.size() != cartItemIds.size()) {
            throw new CheckoutException(
                    HttpStatus.BAD_REQUEST,
                    "CART_ITEM_NOT_FOUND",
                    "Có sản phẩm không tồn tại hoặc không thuộc giỏ hàng"
            );
        }

        Set<UUID> returnedIds = new HashSet<>();
        for (CartItem item : items) {
            if (item.getId() == null
                    || !cartItemIds.contains(item.getId())
                    || !returnedIds.add(item.getId())) {
                throw new CheckoutException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_CART_ITEMS",
                        "Danh sách sản phẩm được chọn không hợp lệ"
                );
            }

            if (item.getVariantId() == null
                    || item.getQuantity() == null
                    || item.getQuantity() <= 0) {
                throw new CheckoutException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_CART_ITEM",
                        "Sản phẩm trong giỏ hàng không hợp lệ"
                );
            }
        }

        return items;
    }

    private Map<UUID, VariantCheckoutInfo> loadVariantMap(List<CartItem> cartItems) {
        Set<UUID> variantIds = cartItems.stream()
                .map(CartItem::getVariantId)
                .collect(Collectors.toSet());

        List<VariantCheckoutInfo> variants =
                productClient.getCheckoutInfo(new UUIDListRequest(new ArrayList<>(variantIds)));

        if (variants == null) {
            throw new CheckoutException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "PRODUCT_SERVICE_UNAVAILABLE",
                    "Không lấy được thông tin sản phẩm"
            );
        }

        Map<UUID, VariantCheckoutInfo> result = new HashMap<>();
        for (VariantCheckoutInfo variant : variants) {
            if (variant == null || variant.variantId() == null) {
                throw new CheckoutException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "INVALID_PRODUCT_RESPONSE",
                        "Product Service trả dữ liệu không hợp lệ"
                );
            }

            if (result.putIfAbsent(variant.variantId(), variant) != null) {
                throw new CheckoutException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "DUPLICATE_PRODUCT_RESPONSE",
                        "Product Service trả trùng variant"
                );
            }
        }

        return result;
    }

    private Map<UUID, List<CartItem>> groupByShop(
            List<CartItem> cartItems,
            Map<UUID, VariantCheckoutInfo> variants
    ) {
        Map<UUID, List<CartItem>> result =
                new TreeMap<>(Comparator.comparing(UUID::toString));

        for (CartItem item : cartItems) {
            UUID shopId = variants.get(item.getVariantId()).shopId();
            result.computeIfAbsent(shopId, ignored -> new ArrayList<>()).add(item);
        }

        return result;
    }

    private Map<UUID, ShippingSelectionRequest> mapShippingSelections(
            List<ShippingSelectionRequest> selections,
            Set<UUID> expectedShopIds,
            boolean validateDisplayedValues
    ) {
        Map<UUID, ShippingSelectionRequest> result = new HashMap<>();
        for (ShippingSelectionRequest selection : selections) {
            if (selection == null
                    || selection.shopId() == null
                    || selection.shippingMethod() == null
                    || selection.shippingMethod().isBlank()
                    || (validateDisplayedValues
                    && (selection.displayedShippingFee() == null
                    || selection.displayedShippingFee().signum() < 0
                    || selection.displayedSubtotal() == null
                    || selection.displayedSubtotal().signum() < 0))) {
                throw new CheckoutException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_SHIPPING_SELECTION",
                        "Thông tin vận chuyển không hợp lệ"
                );
            }

            if (result.putIfAbsent(selection.shopId(), selection) != null) {
                throw new CheckoutException(
                        HttpStatus.BAD_REQUEST,
                        "DUPLICATE_SHIPPING_SELECTION",
                        "Một shop có nhiều lựa chọn vận chuyển"
                );
            }
        }

        if (!result.keySet().equals(expectedShopIds)) {
            throw new CheckoutException(
                    HttpStatus.BAD_REQUEST,
                    "SHIPPING_SELECTION_MISMATCH",
                    "Danh sách shop và phương thức vận chuyển không khớp"
            );
        }

        return result;
    }

    private Map<UUID, ShippingSelectionRequest> defaultShippingSelections(
            Set<UUID> shopIds
    ) {
        Map<UUID, ShippingSelectionRequest> result = new HashMap<>();
        for (UUID shopId : shopIds) {
            result.put(
                    shopId,
                    new ShippingSelectionRequest(shopId, "STANDARD", null, null)
            );
        }
        return result;
    }

    private Map<UUID, ShopInformationResponse> loadShopMap(Set<UUID> shopIds) {
        List<ShopInformationResponse> shops =
                userClient.getShopsByIds(new UUIDListRequest(new ArrayList<>(shopIds)));

        if (shops == null) {
            throw new CheckoutException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SHOP_SERVICE_UNAVAILABLE",
                    "Không lấy được thông tin shop"
            );
        }

        Map<UUID, ShopInformationResponse> result = new HashMap<>();
        for (ShopInformationResponse shop : shops) {
            if (shop == null
                    || shop.id() == null
                    || !shopIds.contains(shop.id())
                    || result.putIfAbsent(shop.id(), shop) != null) {
                throw new CheckoutException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "INVALID_SHOP_RESPONSE",
                        "Thông tin shop không hợp lệ"
                );
            }
        }

        return result;
    }

    private CheckoutContext.Receiver buildReceiver(CheckoutAddressResponse address) {
        if (address == null
                || address.receiverName() == null
                || address.receiverName().isBlank()
                || address.receiverPhone() == null
                || address.receiverPhone().isBlank()
                || address.fullAddress() == null
                || address.fullAddress().isBlank()) {
            throw new CheckoutException(
                    HttpStatus.CONFLICT,
                    "INVALID_RECEIVER_ADDRESS",
                    "Địa chỉ nhận hàng chưa đầy đủ"
            );
        }

        if (!validCoordinates(address.latitude(), address.longitude())) {
            throw new CheckoutException(
                    HttpStatus.CONFLICT,
                    "INVALID_RECEIVER_COORDINATES",
                    "Địa chỉ nhận hàng chưa có tọa độ hợp lệ"
            );
        }

        return new CheckoutContext.Receiver(
                address.receiverName(),
                address.receiverPhone(),
                address.fullAddress(),
                address.latitude(),
                address.longitude()
        );
    }

    private List<OrderSeed> buildOrderSeeds(
            Map<UUID, List<CartItem>> itemsByShop,
            Map<UUID, VariantCheckoutInfo> variants,
            Map<UUID, ShopInformationResponse> shops,
            Map<UUID, ShippingSelectionRequest> selections,
            boolean validateDisplayedSubtotal
    ) {
        List<OrderSeed> result = new ArrayList<>();

        for (Map.Entry<UUID, List<CartItem>> entry : itemsByShop.entrySet()) {
            UUID shopId = entry.getKey();
            ShopInformationResponse shop = shops.get(shopId);
            UUID orderId = UUID.randomUUID();

            List<CheckoutContext.OrderItemDraft> itemDrafts = entry.getValue()
                    .stream()
                    .sorted(Comparator.comparing(item -> item.getId().toString()))
                    .map(item -> buildItemDraft(item, variants.get(item.getVariantId())))
                    .toList();

            BigDecimal subtotal = itemDrafts.stream()
                    .map(CheckoutContext.OrderItemDraft::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            ShippingSelectionRequest selection = selections.get(shopId);
            if (validateDisplayedSubtotal
                    && subtotal.compareTo(selection.displayedSubtotal()) != 0) {
                throw new CheckoutException(
                        HttpStatus.CONFLICT,
                        "PRODUCT_PRICE_CHANGED",
                        "Giá sản phẩm đã thay đổi, vui lòng cập nhật checkout"
                );
            }

            result.add(new OrderSeed(
                    orderId,
                    shopId,
                    shop.name(),
                    subtotal,
                    List.copyOf(itemDrafts)
            ));
        }

        return result;
    }

    private CheckoutContext.OrderItemDraft buildItemDraft(
            CartItem item,
            VariantCheckoutInfo variant
    ) {
        UUID orderItemId = UUID.randomUUID();
        BigDecimal subtotal =
                variant.price().multiply(BigDecimal.valueOf(item.getQuantity()));

        return new CheckoutContext.OrderItemDraft(
                orderItemId,
                item.getId(),
                variant.productId(),
                variant.variantId(),
                variant.productName(),
                variant.variantName(),
                variant.sku(),
                variant.primaryImageUrl(),
                variant.price(),
                item.getQuantity(),
                subtotal,
                variant.weightGram(),
                variant.lengthCm(),
                variant.widthCm(),
                variant.heightCm()
        );
    }

    private boolean validCoordinates(BigDecimal latitude, BigDecimal longitude) {
        return latitude != null
                && longitude != null
                && latitude.compareTo(BigDecimal.valueOf(-90)) >= 0
                && latitude.compareTo(BigDecimal.valueOf(90)) <= 0
                && longitude.compareTo(BigDecimal.valueOf(-180)) >= 0
                && longitude.compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    public record CheckoutData(
            Map<UUID, VariantCheckoutInfo> variants,
            Map<UUID, List<CartItem>> itemsByShop,
            Map<UUID, ShippingSelectionRequest> selections,
            Map<UUID, ShopInformationResponse> shops,
            CheckoutAddressResponse address,
            CheckoutContext.Receiver receiver,
            List<OrderSeed> orderSeeds
    ) {
    }

    public record OrderSeed(
            UUID orderId,
            UUID shopId,
            String shopName,
            BigDecimal subtotal,
            List<CheckoutContext.OrderItemDraft> items
    ) {
    }
}
