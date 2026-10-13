package com.karnoshipping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class KarnoShippingApplication {
    public static void main(String[] args) {
        SpringApplication.run(KarnoShippingApplication.class, args);
    }
}
