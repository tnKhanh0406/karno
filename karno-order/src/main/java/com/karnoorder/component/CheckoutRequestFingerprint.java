package com.karnoorder.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.karnoorder.dto.request.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
public class CheckoutRequestFingerprint {

    private final ObjectMapper objectMapper;

    private record CanonicalShipping(
            String shopId,
            String shippingMethod,
            String displayedShippingFee,
            String displayedSubtotal
    ) {}

    private record CanonicalRequest(
            List<String> cartItemIds,
            String addressId,
            List<CanonicalShipping> shippingSelections,
            String paymentMethod,
            String note,
            Instant deliverySlotStart,
            Instant deliverySlotEnd
    ) {}


    public String hash(CreateOrderRequest request) {
        List<String> cartIds =
                request.cartItemIds()
                        .stream()
                        .map(UUID::toString)
                        .sorted()
                        .toList();

        List<CanonicalShipping> shipping =
                request.shippingSelections()
                        .stream()
                        .map(selection ->
                                new CanonicalShipping(
                                        selection.shopId().toString(),
                                        selection.shippingMethod().trim().toUpperCase(Locale.ROOT),
                                        normalizeMoney(selection.displayedShippingFee()),
                                        normalizeMoney(selection.displayedSubtotal())
                                )
                        )
                        .sorted(
                                Comparator.comparing(CanonicalShipping::shopId)
                                        .thenComparing(CanonicalShipping::shippingMethod)
                        )
                        .toList();

        CanonicalRequest canonical =
                new CanonicalRequest(
                        cartIds,
                        request.addressId().toString(),
                        shipping,
                        request.paymentMethod().name(),
                        request.note(),
                        request.deliverySlotStart(),
                        request.deliverySlotEnd()
                );

        try {
            byte[] json = objectMapper.writeValueAsBytes(canonical);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json);
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to generate checkout request hash", ex);
        }
    }

    private String normalizeMoney(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
