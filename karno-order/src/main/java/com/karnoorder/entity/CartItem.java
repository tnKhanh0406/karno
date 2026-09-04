package com.karnoorder.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private UUID id;
    private UUID cartId;
    private UUID variantId;
    private UUID productId;
    private UUID shopId;
    private Integer quantity;
}