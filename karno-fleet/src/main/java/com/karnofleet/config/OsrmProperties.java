package com.karnofleet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routing.osrm")
public record OsrmProperties(
        String baseUrl,
        String profile

) {
}
