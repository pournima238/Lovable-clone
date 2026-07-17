package com.example.account_service.services;


import com.example.account_service.dto.Subscriptions.CheckoutRequest;
import com.example.account_service.dto.Subscriptions.CheckoutResponse;
import com.example.account_service.dto.Subscriptions.PortalResponse;
import com.stripe.model.StripeObject;

import java.util.Map;

public interface PaymentProcessor {

    CheckoutResponse createCheckoutSession(CheckoutRequest request, Long userId);

    PortalResponse openCustomerPortal(Long userId);

    void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata);
}
