package com.karnoshipping.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "shipping.pricing")
public class ShippingPricingProperties {

    private Map<String, MethodRule> methods = new HashMap<>();

    @Data
    public static class MethodRule {
        private BigDecimal baseFee;
        private double includedDistanceKm;
        private BigDecimal feePerKm;
        private int includedWeightGram;
        private BigDecimal extraFeePer500g;
        private long estimatedHours;
    }

    public MethodRule getRule(String shippingMethod) {
        if (shippingMethod == null || shippingMethod.isBlank()) {
            return null;
        }

        String method = shippingMethod
                .trim()
                .toLowerCase(Locale.ROOT);

        return methods.get(method);
    }

    public boolean supports(String shippingMethod) {
        return getRule(shippingMethod) != null;
    }

    @PostConstruct
    public void init() {
        log.info(
                "Loaded shipping pricing methods: {}",
                methods.keySet()
        );
        if (methods.isEmpty()) {
            throw new IllegalStateException(
                    "No shipping pricing methods configured"
            );
        }
    }
}