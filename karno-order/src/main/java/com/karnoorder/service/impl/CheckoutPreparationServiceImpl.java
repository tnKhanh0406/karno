package com.karnoorder.service.impl;

import com.karnoorder.client.KarnoProductClient;
import com.karnoorder.client.KarnoShippingClient;
import com.karnoorder.client.KarnoUserClient;
import com.karnoorder.constants.EventTopics;
import com.karnoorder.constants.EventTypes;
import com.karnoorder.dto.request.CreateOrderRequest;
import com.karnoorder.dto.request.ShippingRateRequest;
import com.karnoorder.dto.request.ShippingSelectionRequest;
import com.karnoorder.dto.request.UUIDListRequest;
import com.karnoorder.dto.response.*;
import com.karnoorder.entity.CartItem;
import com.karnoorder.entity.CheckoutSaga;
import com.karnoorder.enums.CheckoutSagaStatus;
import com.karnoorder.exception.CheckoutException;
import com.karnoorder.messaging.payload.InventoryReservationRequestedPayload;
import com.karnoorder.repository.CartItemRepo;
import com.karnoorder.repository.CheckoutSagaRepo;
import com.karnoorder.service.CheckoutPreparationService;
import com.karnoorder.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutPreparationServiceImpl implements CheckoutPreparationService {
    private final CheckoutSagaRepo checkoutSagaRepo;
    private final OutboxEventService outboxService;
    private final CartItemRepo cartItemRepo;
    private final KarnoProductClient productClient;
    private final KarnoUserClient userClient;
    private final KarnoShippingClient shippingClient;

    @Override
    @Transactional
    public UUID startTestSaga(UUID customerId, UUID variantId, Integer quantity, String idempotencyKey) {
        UUID checkoutId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();

        CheckoutSaga checkoutSaga = CheckoutSaga.builder()
                .id(checkoutId)
                .customerId(customerId)
                .idempotencyKey(idempotencyKey)
                .status(CheckoutSagaStatus.PROCESSING)
                .contextPayload("{}")
                .version(0L)
                .build();

        checkoutSagaRepo.insert(checkoutSaga);

        InventoryReservationRequestedPayload payload =
                new InventoryReservationRequestedPayload(
                        checkoutId,
                        List.of(
                                new InventoryReservationRequestedPayload.Item(
                                        orderId,
                                        orderItemId,
                                        variantId,
                                        quantity
                                )
                        )
                );

        outboxService.enqueueEvent(
                "CHECKOUT",
                checkoutId,
                EventTypes.INVENTORY_RESERVATION_REQUESTED,
                EventTopics.INVENTORY_RESERVATION_REQUESTED,
                payload
        );

        return checkoutId;
    }

    private record OrderSeed(
            UUID orderId,
            UUID shopId,
            String shopName,
            BigDecimal subtotal,
            List<CheckoutContext.OrderItemDraft> items
    ) {}

    @Override
    public CheckoutContext prepare(UUID customerId, CreateOrderRequest request) {

        List<CartItem> cartItems = loadCartItems(customerId, request.cartItemIds());

        Map<UUID, VariantCheckoutInfo> variantMap = loadVariantMap(cartItems);
        Map<UUID, List<CartItem>> itemsByShop = groupByShop(cartItems, variantMap);
        Map<UUID, ShippingSelectionRequest> selections = mapShippingSelections(request.shippingSelections(), itemsByShop.keySet());
        Map<UUID, ShopInformationResponse> shopMap = loadShopMap(itemsByShop.keySet());

        CheckoutAddressResponse address = userClient.getUserAddressById(request.addressId());
        CheckoutContext.Receiver receiver = buildReceiver(address);

        List<OrderSeed> seeds =
                buildOrderSeeds(
                        itemsByShop,
                        variantMap,
                        shopMap,
                        selections
                );

        ShippingRateRequest shippingRequest =
                buildShippingRequest(
                        address,
                        seeds,
                        selections
                );

        ShippingRateResponse shippingResponse = shippingClient.calculate(shippingRequest);

        Map<UUID, ShippingRateResponse.Rate> rateMap = mapRates(shippingResponse, itemsByShop.keySet());

        List<CheckoutContext.OrderDraft> orderDrafts =
                buildFinalOrders(
                        seeds,
                        rateMap,
                        selections
                );

        UUID checkoutId = UUID.randomUUID();

        return new CheckoutContext(
                checkoutId,
                customerId,
                receiver,
                request.paymentMethod(),
                request.note(),
                request.deliverySlotStart(),
                request.deliverySlotEnd(),
                Instant.now(),
                List.copyOf(orderDrafts)
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
            if (item.getId() == null || !cartItemIds.contains(item.getId()) || !returnedIds.add(item.getId())) {
                throw new CheckoutException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_CART_ITEMS",
                        "Danh sách sản phẩm được chọn không hợp lệ"
                );
            }

            if (item.getVariantId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
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

        List<VariantCheckoutInfo> variants = productClient.getCheckoutInfo(new UUIDListRequest(new ArrayList<>(variantIds)));

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
            Map<UUID, VariantCheckoutInfo> variantMap
    ) {
        Map<UUID, List<CartItem>> result = new TreeMap<>(Comparator.comparing(UUID::toString));

        for (CartItem item : cartItems) {
            UUID shopId = variantMap.get(item.getVariantId()).shopId();
            result.computeIfAbsent(shopId, ignored -> new ArrayList<>()).add(item);
        }

        return result;
    }

    private Map<UUID, ShippingSelectionRequest> mapShippingSelections(
            List<ShippingSelectionRequest> selections,
            Set<UUID> expectedShopIds
    ) {
        Map<UUID, ShippingSelectionRequest> result = new HashMap<>();

        for (ShippingSelectionRequest selection : selections) {
            if (selection == null
                    || selection.shopId() == null
                    || selection.shippingMethod() == null
                    || selection.shippingMethod().isBlank()
                    || selection.displayedShippingFee() == null
                    || selection.displayedShippingFee().signum() < 0
                    || selection.displayedSubtotal() == null
                    || selection.displayedSubtotal().signum() < 0) {
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

    private Map<UUID, ShopInformationResponse> loadShopMap(Set<UUID> shopIds) {
        List<ShopInformationResponse> shops = userClient.getShopsByIds(new UUIDListRequest(new ArrayList<>(shopIds)));

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

    private boolean validCoordinates(BigDecimal latitude, BigDecimal longitude) {
        return latitude != null
                && longitude != null
                && latitude.compareTo(BigDecimal.valueOf(-90)) >= 0
                && latitude.compareTo(BigDecimal.valueOf(90)) <= 0
                && longitude.compareTo(BigDecimal.valueOf(-180)) >= 0
                && longitude.compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    private List<OrderSeed> buildOrderSeeds(
            Map<UUID, List<CartItem>> itemsByShop,
            Map<UUID, VariantCheckoutInfo> variantMap,
            Map<UUID, ShopInformationResponse> shopMap,
            Map<UUID, ShippingSelectionRequest> selections
    ) {

        List<OrderSeed> result = new ArrayList<>();

        for (Map.Entry<UUID, List<CartItem>> entry : itemsByShop.entrySet()) {
            UUID shopId = entry.getKey();
            ShopInformationResponse shop = shopMap.get(shopId);
            UUID orderId = UUID.randomUUID();

            List<CheckoutContext.OrderItemDraft> itemDrafts =
                    entry.getValue()
                            .stream()
                            .sorted(Comparator.comparing(item -> item.getId().toString()))
                            .map(item -> buildItemDraft(item, variantMap.get(item.getVariantId())))
                            .toList();

            BigDecimal subtotal =
                    itemDrafts.stream()
                            .map(CheckoutContext.OrderItemDraft::subtotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            ShippingSelectionRequest selection = selections.get(shopId);

            // Phát hiện giá sản phẩm thay đổi so với trang checkout
            if (subtotal.compareTo(selection.displayedSubtotal()) != 0) {
                throw new CheckoutException(
                        HttpStatus.CONFLICT,
                        "PRODUCT_PRICE_CHANGED",
                        "Giá sản phẩm đã thay đổi, vui lòng cập nhật checkout"
                );
            }

            result.add(
                    new OrderSeed(
                            orderId,
                            shopId,
                            shop.name(),
                            subtotal,
                            List.copyOf(itemDrafts)
                    )
            );
        }

        return result;
    }

    private CheckoutContext.OrderItemDraft buildItemDraft(
            CartItem item,
            VariantCheckoutInfo variant
    ) {

        UUID orderItemId = UUID.randomUUID();

        BigDecimal subtotal = variant.price().multiply(BigDecimal.valueOf(item.getQuantity()));

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

    private ShippingRateRequest buildShippingRequest(
            CheckoutAddressResponse address,
            List<OrderSeed> seeds,
            Map<UUID, ShippingSelectionRequest> selections
    ) {

        ShippingRateRequest.ShippingDropoff dropoff =
                new ShippingRateRequest.ShippingDropoff(
                        address.addressId(),
                        address.receiverName(),
                        address.receiverPhone(),
                        address.fullAddress(),
                        address.latitude(),
                        address.longitude()
                );

        List<ShippingRateRequest.ShipmentRequest> shipments = new ArrayList<>();

        for (OrderSeed seed : seeds) {
            ShippingSelectionRequest selection = selections.get(seed.shopId());

            List<ShippingRateRequest.Item> items =
                    seed.items()
                            .stream()
                            .map(item ->
                                    new ShippingRateRequest.Item(
                                            item.orderItemId(),
                                            item.variantId(),
                                            item.quantity(),
                                            item.weightGram(),
                                            item.lengthCm(),
                                            item.widthCm(),
                                            item.heightCm()
                                    )
                            )
                            .toList();

            shipments.add(
                    new ShippingRateRequest.ShipmentRequest(
                            seed.shopId(),
                            normalizeMethod(
                                    selection.shippingMethod()
                            ),
                            items
                    )
            );
        }

        return new ShippingRateRequest(dropoff, List.copyOf(shipments));
    }

    private String normalizeMethod(String method) {
        return method.trim().toUpperCase(Locale.ROOT);
    }

    private Map<UUID, ShippingRateResponse.Rate> mapRates(
            ShippingRateResponse response,
            Set<UUID> expectedShopIds
    ) {
        if (response == null || response.rates() == null) {
            throw new CheckoutException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SHIPPING_RATE_UNAVAILABLE",
                    "Không lấy được phí vận chuyển"
            );
        }

        Map<UUID, ShippingRateResponse.Rate> result = new HashMap<>();

        for (ShippingRateResponse.Rate rate : response.rates()) {
            if (rate == null || rate.shopId() == null || !expectedShopIds.contains(rate.shopId())) {
                throw new CheckoutException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "INVALID_SHIPPING_RESPONSE",
                        "Shipping Service trả dữ liệu không hợp lệ"
                );
            }

            if (result.putIfAbsent(rate.shopId(), rate) != null) {
                throw new CheckoutException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "DUPLICATE_SHIPPING_RATE",
                        "Shipping Service trả trùng shop"
                );
            }
        }

        if (!result.keySet().equals(expectedShopIds)) {
            throw new CheckoutException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "MISSING_SHIPPING_RATE",
                    "Thiếu phí vận chuyển cho một shop"
            );
        }

        return result;
    }

    private List<CheckoutContext.OrderDraft> buildFinalOrders(
            List<OrderSeed> seeds,
            Map<UUID, ShippingRateResponse.Rate> rateMap,
            Map<UUID, ShippingSelectionRequest> selections
    ) {

        List<CheckoutContext.OrderDraft> result = new ArrayList<>();
        for (OrderSeed seed : seeds) {
            ShippingRateResponse.Rate rate = rateMap.get(seed.shopId());
            ShippingSelectionRequest selection = selections.get(seed.shopId());

            if (rate.shippingMethod() == null || !normalizeMethod(rate.shippingMethod()).equals(normalizeMethod(selection.shippingMethod()))) {
                throw new CheckoutException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "SHIPPING_METHOD_MISMATCH",
                        "Phương thức vận chuyển không khớp"
                );
            }

            if (rate.shippingFee() == null
                    || rate.shippingFee().signum() < 0
                    || rate.totalWeightGram() == null
                    || rate.totalWeightGram() <= 0) {

                throw new CheckoutException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "INVALID_SHIPPING_RATE",
                        "Dữ liệu phí vận chuyển không hợp lệ"
                );
            }

            if (rate.shippingFee().compareTo(selection.displayedShippingFee()) != 0) {
                throw new CheckoutException(
                        HttpStatus.CONFLICT,
                        "SHIPPING_FEE_CHANGED",
                        "Phí vận chuyển đã thay đổi, vui lòng cập nhật checkout"
                );
            }

            ShippingRateResponse.PickupSnapshot pickup = rate.pickup();

            if (pickup == null
                    || pickup.address() == null
                    || pickup.address().isBlank()
                    || !validCoordinates(
                    pickup.latitude(),
                    pickup.longitude()
            )) {
                throw new CheckoutException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "INVALID_PICKUP_LOCATION",
                        "Không lấy được địa chỉ lấy hàng hợp lệ"
                );
            }

            CheckoutContext.Pickup pickupDraft =
                    new CheckoutContext.Pickup(
                            pickup.name(),
                            pickup.phone(),
                            pickup.address(),

                            pickup.latitude(),
                            pickup.longitude()
                    );

            CheckoutContext.ShippingSnapshot shipping =
                    new CheckoutContext.ShippingSnapshot(
                            normalizeMethod(rate.shippingMethod()),
                            rate.shippingFee(),
                            rate.totalWeightGram(),
                            rate.estimatedDeliveryAt(),
                            pickupDraft
                    );

            BigDecimal totalAmount = seed.subtotal().add(shipping.shippingFee());

            result.add(
                    new CheckoutContext.OrderDraft(
                            seed.orderId(),
                            seed.shopId(),
                            seed.shopName(),
                            shipping,
                            seed.subtotal(),
                            totalAmount,
                            seed.items()
                    )
            );
        }

        return result;
    }
}
