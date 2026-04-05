package com.example.aiproject.lovable_clone.service;

import com.example.aiproject.lovable_clone.dto.Subscriptions.CheckoutRequest;
import com.example.aiproject.lovable_clone.dto.Subscriptions.CheckoutResponse;
import com.example.aiproject.lovable_clone.dto.Subscriptions.PortalResponse;
import com.example.aiproject.lovable_clone.dto.Subscriptions.SubscriptionResponse;

public interface SubscriptionService {
    SubscriptionResponse getCurrentSubscription(Long userId);

    CheckoutResponse createCheckoutSession(CheckoutRequest request, Long userId);

    PortalResponse openCustomerPortal(Long userId);
}
