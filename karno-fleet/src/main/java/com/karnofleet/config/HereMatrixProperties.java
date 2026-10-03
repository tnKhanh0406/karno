package com.karnofleet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "here.matrix")
public class HereMatrixProperties {

    private String baseUrl;
    private String apiKey;

    private String transportMode;
    private String routingMode = "fast";

    private int regionMarginMeters;
    private String zoneId;

    private Vehicle vehicle = new Vehicle();

    @Data
    public static class Vehicle {

        private int currentWeightKg;
        private int grossWeightKg;
    }
}