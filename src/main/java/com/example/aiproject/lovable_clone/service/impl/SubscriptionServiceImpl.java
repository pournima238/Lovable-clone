package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.Subscriptions.*;
import com.example.aiproject.lovable_clone.entity.Plan;
import com.example.aiproject.lovable_clone.entity.Subscription;
import com.example.aiproject.lovable_clone.entity.User;
import com.example.aiproject.lovable_clone.enums.SubscriptionStatus;
import com.example.aiproject.lovable_clone.error.ResourceNotFoundException;
import com.example.aiproject.lovable_clone.mapper.SubscriptionMapper;
import com.example.aiproject.lovable_clone.repository.PlanRepository;
import com.example.aiproject.lovable_clone.repository.SubscriptionRepository;
import com.example.aiproject.lovable_clone.repository.UserRepository;
import com.example.aiproject.lovable_clone.security.AuthUtil;
import com.example.aiproject.lovable_clone.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
    private final AuthUtil authUtil;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;

    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    @Override
    public SubscriptionResponse getCurrentSubscription(Long userId) {
        return subscriptionRepository.findByUserIdAndStatusIn(
                        userId,
                        Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE, SubscriptionStatus.TRIALING)
                )
                // 1. Map the Entity to the DTO if it exists
                .map(subscriptionMapper::toSubscriptionResponse)
                // 2. Provide the default DTO if the Optional is empty
                .orElseGet(() -> new SubscriptionResponse(
                        new PlanResponse(null, null, null, null, null, null, null),
                        "none",
                        null,
                        0L // 3. Use '0L' instead of '0' since tokenUsedThisCycle is a Long
                ));
    }

    @Override
    public void activateSubscription(Long userId, Long planId, String subscriptionId, String customerId) {
        boolean exists = subscriptionRepository.existsByStripeSubscriptionId(subscriptionId);
        if (exists) return;
        User user = getUser(userId);
        Plan plan = getPlan(planId);
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .stripeSubscriptionId(subscriptionId)
                .status(SubscriptionStatus.INCOMPLETE)
                .build();
        subscriptionRepository.save(subscription);
    }

    @Override
    public void updateSubscription(String subscriptionId, SubscriptionStatus status, Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, Long planId) {
        Subscription subscription = getSubscription(subscriptionId);
        Instant newStart = periodStart != null ? periodStart : subscription.getCurrentPeriodEnd();
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscriptionRepository.save(subscription);
    }

    @Override
    public void cancelSubscription(String id) {

    }

    @Override
    public void renewSubscription(String subId, Instant periodStart, Instant periodEnd) {
        Subscription subscription = getSubscription(subId);
        Instant newStart = periodStart != null ? periodStart : subscription.getCurrentPeriodEnd();
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(periodEnd);
        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE || subscription.getStatus() == SubscriptionStatus.INCOMPLETE) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }
        subscriptionRepository.save(subscription);
    }

    @Override
    public void markSubscriptionPastDue(String subId) {

    }

    //Utility methods
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user", userId.toString()));
    }

    private Plan getPlan(Long planId) {
        return planRepository.findById(planId).orElseThrow(() -> new ResourceNotFoundException("plan", planId.toString()));
    }

    private @NonNull Subscription getSubscription(String subscriptionId) {
        return subscriptionRepository.findByStripeSubscriptionId(subscriptionId).orElseThrow(() -> new ResourceNotFoundException("Subscription",
                subscriptionId));
    }

}
