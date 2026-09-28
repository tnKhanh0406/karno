package com.karnohub.entity;

import com.karnohub.enums.HubStatus;
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
public class Hub {
    private UUID id;
    private String hubCode;
    private String name;
    private Integer minTransferMinutes;
    private HubStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
