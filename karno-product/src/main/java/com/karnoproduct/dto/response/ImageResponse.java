package com.karnoproduct.dto.response;

import com.karnoproduct.enums.ImageType;

import java.util.UUID;

public record ImageResponse(
        UUID id,
        ImageType imageType,
        String url
) {
}
