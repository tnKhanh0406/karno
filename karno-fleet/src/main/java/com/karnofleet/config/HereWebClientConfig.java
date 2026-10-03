package com.karnofleet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HereWebClientConfig {

    @Bean
    public WebClient hereMatrixWebClient(
            HereMatrixProperties properties
    ) {
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}