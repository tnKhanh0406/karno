package com.karnoshipping.service.impl;

import com.karnoshipping.client.KarnoUserClient;
import com.karnoshipping.component.ShippingRateProvider;
import com.karnoshipping.config.ShippingPricingProperties;
import com.karnoshipping.dto.request.ShippingRateRequest;
import com.karnoshipping.dto.response.PackageSummary;
import com.karnoshipping.dto.response.PickupLocationResponse;
import com.karnoshipping.dto.response.ShippingRate;
import com.karnoshipping.dto.response.ShippingRateResponse;
import com.karnoshipping.exception.ShippingException;
import com.karnoshipping.service.ShippingRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShippingRateServiceImpl implements ShippingRateService {

    private final KarnoUserClient karnoUserClient;

    private final ShippingRateProvider rateProvider;

    private final ShippingPricingProperties properties;

    @Override
    public ShippingRateResponse calculate(ShippingRateRequest request) {
        // 1. Validate request
        validateRequest(request);


        // 2. Extract unique shop IDs
        Set<UUID> shopIds = request.shipments()
                        .stream()
                        .map(ShippingRateRequest.ShipmentRequest::shopId)
                        .collect(Collectors.toSet());

        // 3. Fetch pickup locations for the unique shop IDs
        List<PickupLocationResponse> pickupLocations = karnoUserClient.getPickupLocations(shopIds);

        Map<UUID, PickupLocationResponse> pickupMap = pickupLocations
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        PickupLocationResponse::shopId,
                                        Function.identity()
                                )
                        );


        // 4. Calculate shipping rates for each shipment
        List<ShippingRateResponse.Rate> rates = new ArrayList<>();

        for (ShippingRateRequest.ShipmentRequest shipment : request.shipments()) {

            PickupLocationResponse pickup = pickupMap.get(shipment.shopId());
            if (pickup == null) {
                throw new ShippingException("PICKUP_LOCATION_NOT_FOUND");
            }

            PackageSummary packageSummary = buildPackageSummary(shipment.items());

            ShippingRate rate = rateProvider.calculate(
                        pickup,
                        request.dropoff(),
                        packageSummary,
                        shipment.shippingMethod()
            );

            rates.add(
                toResponse(
                    shipment,
                    pickup,
                    packageSummary,
                    rate
                )
            );
        }

        return new ShippingRateResponse(rates);
    }

    private PackageSummary buildPackageSummary(
            List<ShippingRateRequest.Item> items
    ) {

        int totalWeightGram = 0;

        for (ShippingRateRequest.Item item : items) {
            try {
                int itemWeight =
                        Math.multiplyExact(
                                item.weightGram(),
                                item.quantity()
                        );

                totalWeightGram =
                        Math.addExact(
                                totalWeightGram,
                                itemWeight
                        );
            } catch (ArithmeticException ex) {
                throw new ShippingException("PACKAGE_WEIGHT_TOO_LARGE");
            }
        }

        return new PackageSummary(totalWeightGram, items);
    }

    private void validateRequest(ShippingRateRequest request) {

        if (request == null) {
            throw new ShippingException("INVALID_REQUEST");
        }

        if (request.dropoff() == null) {
            throw new ShippingException("DROPOFF_REQUIRED");
        }

        validateCoordinates(
                request.dropoff().latitude(),
                request.dropoff().longitude()
        );

        if (request.shipments() == null || request.shipments().isEmpty()) {
            throw new ShippingException("SHIPMENTS_REQUIRED");
        }

        // Validate unique shop IDs
        Set<UUID> shopIds = new HashSet<>();

        for (ShippingRateRequest.ShipmentRequest shipment : request.shipments()) {
            if (shipment.shopId() == null) {
                throw new ShippingException("SHOP_ID_REQUIRED");
            }

            if (!shopIds.add(shipment.shopId())) {
                throw new ShippingException("DUPLICATE_SHOP");
            }

            if (shipment.shippingMethod() == null || shipment.shippingMethod().isBlank()) {
                throw new ShippingException("SHIPPING_METHOD_REQUIRED");
            }

            if (!properties.supports(shipment.shippingMethod())) {
                throw new ShippingException("UNSUPPORTED_SHIPPING_METHOD");
            }

            if (shipment.items() == null || shipment.items().isEmpty()) {
                throw new ShippingException("SHIPMENT_ITEMS_REQUIRED");
            }

            validateItems(shipment.items());
        }
    }

    private void validateItems(
            List<ShippingRateRequest.Item> items
    ) {

        Set<UUID> variantIds = new HashSet<>();

        for (ShippingRateRequest.Item item : items) {
            if (item.variantId() == null) {
                throw new ShippingException("VARIANT_ID_REQUIRED");
            }

            if (!variantIds.add(item.variantId())) {
                throw new ShippingException("DUPLICATE_VARIANT");
            }

            if (item.quantity() == null || item.quantity() <= 0) {
                throw new ShippingException("INVALID_QUANTITY");
            }

            if (item.weightGram() == null || item.weightGram() <= 0) {
                throw new ShippingException("INVALID_WEIGHT");
            }

            validatePositiveDimension(item.lengthCm());
            validatePositiveDimension(item.widthCm());
            validatePositiveDimension(item.heightCm());
        }
    }

    private void validatePositiveDimension(BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ShippingException("INVALID_PACKAGE_DIMENSION");
        }
    }

    private void validateCoordinates(
            BigDecimal latitude,
            BigDecimal longitude
    ) {

        if (latitude == null || longitude == null) {
            throw new ShippingException("DROPOFF_COORDINATES_REQUIRED");
        }

        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new ShippingException("INVALID_LATITUDE");
        }

        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new ShippingException("INVALID_LONGITUDE");
        }
    }

    private ShippingRateResponse.Rate toResponse(
            ShippingRateRequest.ShipmentRequest shipment,
            PickupLocationResponse pickup,
            PackageSummary packageSummary,
            ShippingRate rate
    ) {
        ShippingRateResponse.PickupSnapshot pickupSnapshot =
                new ShippingRateResponse.PickupSnapshot(
                        pickup.name(),
                        pickup.phone(),
                        pickup.address(),
                        pickup.latitude(),
                        pickup.longitude()
                );

        return new ShippingRateResponse.Rate(
                shipment.shopId(),
                shipment.shippingMethod().trim().toUpperCase(),
                rate.fee(),
                packageSummary.totalWeightGram(),
                rate.estimatedDeliveryAt(),
                pickupSnapshot
        );
    }
}
