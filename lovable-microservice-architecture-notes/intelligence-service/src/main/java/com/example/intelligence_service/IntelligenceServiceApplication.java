package com.example.intelligence_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import java.util.TimeZone;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class IntelligenceServiceApplication {

	public static void main(String[] args) {
		org.springframework.security.core.context.SecurityContextHolder.setStrategyName(
				org.springframework.security.core.context.SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(IntelligenceServiceApplication.class, args);
	}
	@Bean
	public ChatClient chatClient(ChatClient.Builder builder) {
		return builder.build();
	}

}
