package com.example.aiproject.lovable_clone.service;

import com.example.aiproject.lovable_clone.dto.Subscriptions.CheckoutRequest;
import com.example.aiproject.lovable_clone.dto.Subscriptions.CheckoutResponse;
import com.example.aiproject.lovable_clone.dto.Subscriptions.PortalResponse;
import com.example.aiproject.lovable_clone.dto.Subscriptions.SubscriptionResponse;
import com.example.aiproject.lovable_clone.enums.SubscriptionStatus;

import java.time.Instant;

public interface SubscriptionService {
    SubscriptionResponse getCurrentSubscription(Long userId);

    void activateSubscription(Long userId, Long planId, String subscriptionId, String customerId);

    void updateSubscription(String subscriptionId, SubscriptionStatus status, Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, Long planId);

    void cancelSubscription(String id);

    void renewSubscription(String subId, Instant periodStart, Instant periodEnd);

    void markSubscriptionPastDue(String subId);

    boolean canCreateNewProject();
}
