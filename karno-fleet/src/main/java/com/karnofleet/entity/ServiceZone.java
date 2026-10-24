package com.karnofleet.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ServiceZone {
    private UUID id;
    private UUID hubId;
    private String zoneCode;
    private String name;
    private BigDecimal hubLatitude;
    private BigDecimal hubLongitude;
    private String status;
}
