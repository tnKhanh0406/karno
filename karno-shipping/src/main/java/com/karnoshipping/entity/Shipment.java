package com.karnoshipping.entity;

import com.karnoshipping.enums.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    private UUID id;

    private UUID orderId;
    private UUID checkoutId;

    private UUID customerId;
    private UUID shopId;

    private ShipmentStatus status;
    private String trackingCode;

    private String shippingMethod;

    private BigDecimal shippingFee;
    private Integer totalWeightGram;

    private String packagePayload;

    private String pickupName;
    private String pickupPhone;
    private String pickupAddress;
    private BigDecimal pickupLatitude;
    private BigDecimal pickupLongitude;

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal receiverLatitude;
    private BigDecimal receiverLongitude;

    private Instant estimatedDeliveryAt;

    private Instant pickedUpAt;
    private Instant deliveredAt;

    private Instant createdAt;
    private Instant updatedAt;
}
