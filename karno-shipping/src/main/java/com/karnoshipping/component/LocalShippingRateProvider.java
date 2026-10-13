package com.karnoshipping.component;

import com.karnoshipping.config.ShippingPricingProperties;
import com.karnoshipping.dto.request.ShippingRateRequest;
import com.karnoshipping.dto.response.PackageSummary;
import com.karnoshipping.dto.response.PickupLocationResponse;
import com.karnoshipping.dto.response.ShippingRate;
import com.karnoshipping.exception.ShippingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class LocalShippingRateProvider implements ShippingRateProvider {

    private final ShippingPricingProperties properties;

    @Override
    public ShippingRate calculate(
            PickupLocationResponse pickup,
            ShippingRateRequest.ShippingDropoff dropoff,
            PackageSummary packageSummary,
            String shippingMethod
    ) {
        // 1.Get shipping rule
        ShippingPricingProperties.MethodRule rule = properties.getRule(shippingMethod);

        if (rule == null) {
            throw new ShippingException("UNSUPPORTED_SHIPPING_METHOD");
        }

        // 2. Calculate distance
        double distanceKm = calculateDistanceKm(
                    pickup.latitude(),
                    pickup.longitude(),
                    dropoff.latitude(),
                    dropoff.longitude()
        );

        // 3. Calculate distance fee
        BigDecimal distanceFee = calculateDistanceFee(distanceKm, rule);

        // 4. Calculate weight fee
        BigDecimal weightFee = calculateWeightFee(packageSummary.totalWeightGram(), rule);

        // 5. Calculate total fee
        BigDecimal fee = rule.getBaseFee()
                        .add(distanceFee)
                        .add(weightFee)
                        .setScale(0, RoundingMode.HALF_UP);

        // 6. Calculate estimated delivery time
        Instant estimatedDeliveryAt = Instant.now()
                        .plus(Duration.ofHours(rule.getEstimatedHours()));

        return new ShippingRate(fee, estimatedDeliveryAt);
    }

    private double calculateDistanceKm(
            BigDecimal lat1,
            BigDecimal lon1,
            BigDecimal lat2,
            BigDecimal lon2
    ) {

        final double earthRadiusKm = 6371.0;

        double latitude1 = Math.toRadians(lat1.doubleValue());
        double latitude2 = Math.toRadians(lat2.doubleValue());

        double deltaLatitude = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double deltaLongitude = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());


        double a =
                Math.sin(deltaLatitude / 2)
                        * Math.sin(deltaLatitude / 2)
                        +
                        Math.cos(latitude1)
                                * Math.cos(latitude2)
                                * Math.sin(deltaLongitude / 2)
                                * Math.sin(deltaLongitude / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return earthRadiusKm * c;
    }

    private BigDecimal calculateDistanceFee(
            double distanceKm,
            ShippingPricingProperties.MethodRule rule
    ) {
        double extraDistance = distanceKm - rule.getIncludedDistanceKm();

        if (extraDistance <= 0) {
            return BigDecimal.ZERO;
        }

        long chargedKm = (long) Math.ceil(extraDistance);

        return rule.getFeePerKm()
                .multiply(BigDecimal.valueOf(chargedKm));
    }

    private BigDecimal calculateWeightFee(
            int totalWeightGram,
            ShippingPricingProperties.MethodRule rule
    ) {
        int extraWeight = totalWeightGram - rule.getIncludedWeightGram();

        if (extraWeight <= 0) {
            return BigDecimal.ZERO;
        }

        int blocks = (int) Math.ceil(extraWeight / 500.0);

        return rule.getExtraFeePer500g()
                .multiply(BigDecimal.valueOf(blocks));
    }
}
