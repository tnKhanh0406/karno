package com.karnofleet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@EnableFeignClients
public class KarnoFleetApplication {
    public static void main(String[] args) {
        SpringApplication.run(KarnoFleetApplication.class, args);
    }
}
