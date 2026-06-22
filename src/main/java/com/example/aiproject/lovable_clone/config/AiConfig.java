package com.example.aiproject.lovable_clone.config;

import okhttp3.OkHttpClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class AiConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(OpenAiChatOptions.builder()
                        .toolChoice("none"))  // ← remove .build() here
                .build();
    }
}
