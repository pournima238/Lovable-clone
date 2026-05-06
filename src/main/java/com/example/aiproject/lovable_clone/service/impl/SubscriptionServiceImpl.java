package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.Subscriptions.CheckoutRequest;
import com.example.aiproject.lovable_clone.dto.Subscriptions.CheckoutResponse;
import com.example.aiproject.lovable_clone.dto.Subscriptions.PortalResponse;
import com.example.aiproject.lovable_clone.dto.Subscriptions.SubscriptionResponse;
import com.example.aiproject.lovable_clone.service.SubscriptionService;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {
    @Override
    public SubscriptionResponse getCurrentSubscription(Long userId) {
        return null;
    }

    @Override
    public CheckoutResponse createCheckoutSession(CheckoutRequest request, Long userId) {
        return null;
    }

    @Override
    public PortalResponse openCustomerPortal(Long userId) {
        return null;
    }
}
