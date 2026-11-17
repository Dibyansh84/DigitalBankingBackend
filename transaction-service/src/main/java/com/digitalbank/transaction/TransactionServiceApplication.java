package com.digitalbank.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class TransactionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransactionServiceApplication.class, args);
	}

    /* From Spring Boot 4.1, it is not necessary to annotate your main class with @EnableDiscoveryClient to
    consider the service as a Eureka client because Spring Boot 4.1 by default or internally considers it
    as a discovery client as soon as it finds Eureka Discovery Client dependency in your pom.xml file. */
}
