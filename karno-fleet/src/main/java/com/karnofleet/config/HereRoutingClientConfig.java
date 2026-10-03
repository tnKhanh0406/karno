package com.karnofleet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HereRoutingClientConfig {

    @Bean
    public WebClient hereRoutesWebClient(
            WebClient.Builder builder,
            HereRoutingProperties properties
    ) {
        return builder
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
