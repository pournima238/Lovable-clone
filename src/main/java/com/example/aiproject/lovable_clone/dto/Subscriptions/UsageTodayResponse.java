package com.example.aiproject.lovable_clone.dto.Subscriptions;

public record UsageTodayResponse(
        int tokenUsed,
        int tokensLimit,
        int previewsRunning,
        int previewsLimit
) {
}
