package com.karnofleet.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class OsrmClientConfig {

    @Bean
    public WebClient osrmWebClient(OsrmProperties properties) {

        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
