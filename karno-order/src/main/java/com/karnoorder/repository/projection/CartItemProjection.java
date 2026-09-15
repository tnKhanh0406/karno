package com.karnoorder.repository.projection;

import lombok.Data;

import java.util.UUID;

@Data
public class CartItemProjection {
    private UUID id;
    private UUID productId;
    private UUID variantId;
    private UUID shopId;
    private Integer quantity;
}
