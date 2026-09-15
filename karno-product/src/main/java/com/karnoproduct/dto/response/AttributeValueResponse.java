package com.karnoproduct.dto.response;

import java.util.UUID;

public record AttributeValueResponse(
        UUID id,
        String value
) {
}
