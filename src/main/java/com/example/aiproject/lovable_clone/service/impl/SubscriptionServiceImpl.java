package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.Subscriptions.*;
import com.example.aiproject.lovable_clone.entity.Plan;
import com.example.aiproject.lovable_clone.entity.Subscription;
import com.example.aiproject.lovable_clone.entity.User;
import com.example.aiproject.lovable_clone.enums.SubscriptionStatus;
import com.example.aiproject.lovable_clone.error.ResourceNotFoundException;
import com.example.aiproject.lovable_clone.mapper.SubscriptionMapper;
import com.example.aiproject.lovable_clone.repository.PlanRepository;
import com.example.aiproject.lovable_clone.repository.ProjectMemberRepository;
import com.example.aiproject.lovable_clone.repository.SubscriptionRepository;
import com.example.aiproject.lovable_clone.repository.UserRepository;
import com.example.aiproject.lovable_clone.security.AuthUtil;
import com.example.aiproject.lovable_clone.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {
    private final AuthUtil authUtil;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final ProjectMemberRepository projectMemberRepository;

    private final Integer FREE_TIER_MAX_PROJECTS_ALLOWED = 1;

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
        boolean hasSubscriptionEnded = false;
        if (status != null && status != subscription.getStatus()) {
            subscription.setStatus(status);
            hasSubscriptionEnded = true;
        }
        if (periodStart != null && periodStart != subscription.getCurrentPeriodStart()) {
            subscription.setCurrentPeriodStart(periodStart);
            hasSubscriptionEnded = true;
        }
        if (periodEnd != null && periodEnd != subscription.getCurrentPeriodEnd()) {
            subscription.setCurrentPeriodEnd(periodEnd);
            hasSubscriptionEnded = true;
        }

        if (cancelAtPeriodEnd != null && cancelAtPeriodEnd != subscription.getCancelPeriodEnd()) {
            subscription.setCancelPeriodEnd(cancelAtPeriodEnd);
            hasSubscriptionEnded = true;
        }

        if (periodEnd != null && periodEnd != subscription.getCurrentPeriodEnd()) {
            subscription.setCurrentPeriodEnd(periodEnd);
            hasSubscriptionEnded = true;
        }

        if (planId != null && planId != subscription.getPlan().getId()) {
            Plan plan = getPlan(planId);
            subscription.setPlan(plan);
            hasSubscriptionEnded = true;
        }
        if (hasSubscriptionEnded) {
            subscriptionRepository.save(subscription);
        }
    }

    @Override
    public void cancelSubscription(String id) {
        Subscription subscription = getSubscription(id);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);
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
        Subscription subscription = getSubscription(subId);
        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            log.debug("Subscription is already past due");
            return;
        }
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);
        // you can also write logic to notify users via email
    }

    @Override
    public boolean canCreateNewProject() {
        Long userId = authUtil.getCurrentUserId();
        SubscriptionResponse currentSubscription = getCurrentSubscription(userId);
        int countOfProjectsOwned = projectMemberRepository.countProjectOwnedByUser(userId);
        if (currentSubscription.plan() == null) {
            return countOfProjectsOwned < FREE_TIER_MAX_PROJECTS_ALLOWED;
        }
        return countOfProjectsOwned < currentSubscription.plan().maxProjects();
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
