package com.crm.integration_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class IntegrationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IntegrationServiceApplication.class, args);
	}

}
