package com.example.account_service.dto.Subscriptions;

import com.example.common_lib.dto.PlanDto;

import java.time.Instant;

public record SubscriptionResponse(
        PlanDto plan,
        String status,
        Instant currentPeriodEnd,
        Long tokenUsedThisCycle
) {
}
