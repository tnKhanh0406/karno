package com.karnouser.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {
    private UUID id;
    private UUID userId;
    private String receiverName;
    private String receiverAddress;
    private String receiverPhone;
    private Boolean isDefault;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private OffsetDateTime createdAt;
}