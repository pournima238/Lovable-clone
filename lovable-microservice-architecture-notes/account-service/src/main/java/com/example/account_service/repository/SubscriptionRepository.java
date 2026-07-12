package com.example.account_service.repository;

import com.example.account_service.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.common_lib.enums.SubscriptionStatus;

import java.util.Optional;
import java.util.Set;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserIdAndStatusIn(Long userId, Set<SubscriptionStatus> statusSet);

    boolean existsByStripeSubscriptionId(String subscriptionId);

    Optional<Subscription> findByStripeSubscriptionId(String subscriptionId);
}
