package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.Subscriptions.PlanLimitsResponse;
import com.example.aiproject.lovable_clone.dto.Subscriptions.UsageTodayResponse;
import com.example.aiproject.lovable_clone.service.UsageService;
import org.springframework.stereotype.Service;

@Service
public class UsageServiceImpl implements UsageService {
    @Override
    public UsageTodayResponse getTodayUsage(Long userId) {
        return null;
    }

    @Override
    public PlanLimitsResponse getCurrentSubscriptionLimitsOfUser(Long userId) {
        return null;
    }
}
