package com.localmarket.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LocalMarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(LocalMarketApplication.class, args);
	}

}
