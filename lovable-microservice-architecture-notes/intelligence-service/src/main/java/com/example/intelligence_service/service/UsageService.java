package com.example.intelligence_service.service;



import org.jspecify.annotations.Nullable;

public interface UsageService {
    void recordTokenUsage(Long userId, int actualTokens);

    void checkDailyTokensUsage();
}
