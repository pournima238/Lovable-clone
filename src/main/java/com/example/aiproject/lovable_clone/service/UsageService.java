package com.example.aiproject.lovable_clone.service;

import com.example.aiproject.lovable_clone.dto.Subscriptions.PlanLimitsResponse;
import com.example.aiproject.lovable_clone.dto.Subscriptions.UsageTodayResponse;

public interface UsageService {
    UsageTodayResponse getTodayUsage(Long userId);

    PlanLimitsResponse getCurrentSubscriptionLimitsOfUser(Long userId);
}
