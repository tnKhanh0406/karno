package com.karnoorder.dto.response;

import java.util.UUID;

public record ShopInformationResponse(
        UUID id,
        String name,
        String description,
        String logoUrl
) {
}
