package com.karnouser.dto.request;

import java.util.List;
import java.util.UUID;

public record UUIDListRequest(
       List<UUID> UUIDs
) {
}
