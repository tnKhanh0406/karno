package com.karnoorder.component;

import com.karnoorder.dto.request.CreateOrderRequest;
import com.karnoorder.dto.request.ShippingSelectionRequest;
import com.karnoorder.dto.response.CheckoutContext;
import com.karnoorder.dto.response.ShippingRateResponse;
import com.karnoorder.exception.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class CheckoutContextFactory {

    public CheckoutContext create(
            UUID customerId,
            CreateOrderRequest request,
            CheckoutDataLoader.CheckoutData data,
            ShippingRateService.ShippingResult shipping
    ) {
        List<CheckoutContext.OrderDraft> orders =
                buildOrders(data.orderSeeds(), shipping.rates(), data.selections());

        return new CheckoutContext(
                UUID.randomUUID(),
                customerId,
                data.receiver(),
                request.paymentMethod(),
                request.note(),
                request.deliverySlotStart(),
                request.deliverySlotEnd(),
                Instant.now(),
                List.copyOf(orders)
        );
    }

    private List<CheckoutContext.OrderDraft> buildOrders(
            List<CheckoutDataLoader.OrderSeed> seeds,
            Map<UUID, ShippingRateResponse.Rate> rates,
            Map<UUID, ShippingSelectionRequest> selections
    ) {
        List<CheckoutContext.OrderDraft> result = new ArrayList<>();

        for (CheckoutDataLoader.OrderSeed seed : seeds) {
            ShippingRateResponse.Rate rate = rates.get(seed.shopId());
            ShippingSelectionRequest selection = selections.get(seed.shopId());

            if (rate.shippingMethod() == null
                    || !normalizeMethod(rate.shippingMethod())
                    .equals(normalizeMethod(selection.shippingMethod()))) {
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

            CheckoutContext.Pickup pickup = buildPickup(rate);
            CheckoutContext.ShippingSnapshot shipping =
                    new CheckoutContext.ShippingSnapshot(
                            normalizeMethod(rate.shippingMethod()),
                            rate.shippingFee(),
                            rate.totalWeightGram(),
                            rate.estimatedDeliveryAt(),
                            pickup
                    );

            result.add(new CheckoutContext.OrderDraft(
                    seed.orderId(),
                    seed.shopId(),
                    seed.shopName(),
                    shipping,
                    seed.subtotal(),
                    seed.subtotal().add(shipping.shippingFee()),
                    seed.items()
            ));
        }

        return result;
    }

    private CheckoutContext.Pickup buildPickup(ShippingRateResponse.Rate rate) {
        ShippingRateResponse.PickupSnapshot pickup = rate.pickup();
        if (pickup == null
                || pickup.address() == null
                || pickup.address().isBlank()
                || !validCoordinates(pickup.latitude(), pickup.longitude())) {
            throw new CheckoutException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "INVALID_PICKUP_LOCATION",
                    "Không lấy được địa chỉ lấy hàng hợp lệ"
            );
        }

        return new CheckoutContext.Pickup(
                pickup.name(),
                pickup.phone(),
                pickup.address(),
                pickup.latitude(),
                pickup.longitude()
        );
    }

    private String normalizeMethod(String method) {
        return method.trim().toUpperCase(Locale.ROOT);
    }

    private boolean validCoordinates(BigDecimal latitude, BigDecimal longitude) {
        return latitude != null
                && longitude != null
                && latitude.compareTo(BigDecimal.valueOf(-90)) >= 0
                && latitude.compareTo(BigDecimal.valueOf(90)) <= 0
                && longitude.compareTo(BigDecimal.valueOf(-180)) >= 0
                && longitude.compareTo(BigDecimal.valueOf(180)) <= 0;
    }
}
