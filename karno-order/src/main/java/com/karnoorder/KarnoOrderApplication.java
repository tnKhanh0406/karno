package com.karnoorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class KarnoOrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(KarnoOrderApplication.class, args);
	}

}
