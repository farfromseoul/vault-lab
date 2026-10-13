package com.example.locker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SecretLockerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecretLockerApplication.class, args);
	}

}
