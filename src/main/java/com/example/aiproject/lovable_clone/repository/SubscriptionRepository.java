package com.example.aiproject.lovable_clone.repository;

import com.example.aiproject.lovable_clone.dto.Subscriptions.SubscriptionResponse;
import com.example.aiproject.lovable_clone.entity.Subscription;
import com.example.aiproject.lovable_clone.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserIdAndStatusIn(Long userId, Set<SubscriptionStatus> statusSet);

    boolean existsByStripeSubscriptionId(String subscriptionId);

    Optional<Subscription> findByStripeSubscriptionId(String subscriptionId);
}
