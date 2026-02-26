package com.example.lms_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.lms_backend.config.RsaKeyProperties;
import com.example.lms_backend.config.TokenCleanupProperties;

@SpringBootApplication
@EnableConfigurationProperties({ RsaKeyProperties.class, TokenCleanupProperties.class })
@EnableScheduling
public class LmsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LmsBackendApplication.class, args);
	}

}