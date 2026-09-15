package com.karnouser.dto.response;

import java.util.UUID;

public record ReviewUserResponse(
        UUID id,
        String name,
        String avatarUrl
) {
}
