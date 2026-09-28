package com.karnohub.entity;

import com.karnohub.enums.HubConnectionStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HubConnection {
    private UUID id;
    private UUID fromHubId;
    private UUID toHubId;
    private HubConnectionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
