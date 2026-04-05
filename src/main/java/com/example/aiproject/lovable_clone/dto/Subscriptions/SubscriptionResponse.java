package com.example.aiproject.lovable_clone.dto.Subscriptions;

import java.time.Instant;

public record SubscriptionResponse(
      PlanResponse plan,
      String status,
      Instant periodEnd,
      Long tokenUsedThisCycle
) {
}
