package com.karnoorder.component;

import com.karnoorder.client.KarnoShippingClient;
import com.karnoorder.dto.request.ShippingRateRequest;
import com.karnoorder.dto.request.ShippingSelectionRequest;
import com.karnoorder.dto.response.CheckoutAddressResponse;
import com.karnoorder.dto.response.ShippingRateResponse;
import com.karnoorder.exception.CheckoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShippingRateService {
    private final KarnoShippingClient shippingClient;

    public ShippingResult calculate(CheckoutDataLoader.CheckoutData data) {
        ShippingRateRequest request =
                buildRequest(data.address(), data.orderSeeds(), data.selections());
        ShippingRateResponse response = shippingClient.calculate(request);
        Map<UUID, ShippingRateResponse.Rate> rates =
                mapRates(response, data.itemsByShop().keySet());
        return new ShippingResult(rates);
    }

    private ShippingRateRequest buildRequest(
            CheckoutAddressResponse address,
            List<CheckoutDataLoader.OrderSeed> seeds,
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
        for (CheckoutDataLoader.OrderSeed seed : seeds) {
            ShippingSelectionRequest selection = selections.get(seed.shopId());
            List<ShippingRateRequest.Item> items = seed.items()
                    .stream()
                    .map(item -> new ShippingRateRequest.Item(
                            item.orderItemId(),
                            item.variantId(),
                            item.quantity(),
                            item.weightGram(),
                            item.lengthCm(),
                            item.widthCm(),
                            item.heightCm()
                    ))
                    .toList();

            shipments.add(new ShippingRateRequest.ShipmentRequest(
                    seed.shopId(),
                    normalizeMethod(selection.shippingMethod()),
                    items
            ));
        }

        return new ShippingRateRequest(dropoff, List.copyOf(shipments));
    }

    private Map<UUID, ShippingRateResponse.Rate> mapRates(
            ShippingRateResponse response,
            java.util.Set<UUID> expectedShopIds
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
            if (rate == null
                    || rate.shopId() == null
                    || !expectedShopIds.contains(rate.shopId())) {
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

    private String normalizeMethod(String method) {
        return method.trim().toUpperCase(Locale.ROOT);
    }

    public record ShippingResult(
            Map<UUID, ShippingRateResponse.Rate> rates
    ) {
    }
}
