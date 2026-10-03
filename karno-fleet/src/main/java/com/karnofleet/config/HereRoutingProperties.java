package com.karnofleet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "here.routing")
public class HereRoutingProperties {

    private String baseUrl;

    private String apiKey;

    private String transportMode;

    private String routingMode = "fast";

    private String zoneId = "Asia/Ho_Chi_Minh";

    private boolean scooterAllowHighway = false;

    private Duration timeout = Duration.ofSeconds(10);

    private Integer vehicleCurrentWeightKg;

    private Integer vehicleGrossWeightKg;
}
