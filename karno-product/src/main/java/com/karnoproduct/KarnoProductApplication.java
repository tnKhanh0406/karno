package com.karnoproduct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class KarnoProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(KarnoProductApplication.class, args);
    }
}
