package com.karnoproduct.dto.response;

import java.util.UUID;

public record CategoryFilterResponse(
        UUID id,
        UUID parentId,
        String name,
        String slug,
        String imageUrl
) {
}
