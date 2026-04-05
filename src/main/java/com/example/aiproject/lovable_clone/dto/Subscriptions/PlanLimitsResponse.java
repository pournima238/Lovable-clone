package com.example.aiproject.lovable_clone.dto.Subscriptions;

public record PlanLimitsResponse(
        String planName,
        int maxTokensPerDay,
        int maxProjects,
        boolean unlimitedAi
) {
}
