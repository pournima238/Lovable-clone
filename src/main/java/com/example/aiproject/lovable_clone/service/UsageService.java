package com.example.aiproject.lovable_clone.service;

import com.example.aiproject.lovable_clone.dto.Subscriptions.PlanLimitsResponse;
import com.example.aiproject.lovable_clone.dto.Subscriptions.UsageTodayResponse;

import org.jspecify.annotations.Nullable;

public interface UsageService {
    void recordTokenUsage(Long userId, int actualTokens);

    void checkDailyTokensUsage();
}
