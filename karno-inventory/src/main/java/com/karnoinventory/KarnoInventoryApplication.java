package com.karnoinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KarnoInventoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(KarnoInventoryApplication.class, args);
    }
}
