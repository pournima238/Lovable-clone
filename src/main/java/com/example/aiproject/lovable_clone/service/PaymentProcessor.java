package com.example.aiproject.lovable_clone.service;

import com.example.aiproject.lovable_clone.dto.Subscriptions.CheckoutRequest;
import com.example.aiproject.lovable_clone.dto.Subscriptions.CheckoutResponse;
import com.example.aiproject.lovable_clone.dto.Subscriptions.PortalResponse;
import com.stripe.model.StripeObject;

import java.util.Map;

public interface PaymentProcessor {

    CheckoutResponse createCheckoutSession(CheckoutRequest request, Long userId);

    PortalResponse openCustomerPortal(Long userId);

    void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata);
}
