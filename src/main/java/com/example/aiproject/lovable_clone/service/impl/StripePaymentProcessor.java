package com.example.aiproject.lovable_clone.service.impl;

import com.example.aiproject.lovable_clone.dto.Subscriptions.CheckoutRequest;
import com.example.aiproject.lovable_clone.dto.Subscriptions.CheckoutResponse;
import com.example.aiproject.lovable_clone.dto.Subscriptions.PortalResponse;
import com.example.aiproject.lovable_clone.entity.Plan;
import com.example.aiproject.lovable_clone.entity.User;
import com.example.aiproject.lovable_clone.error.ResourceNotFoundException;
import com.example.aiproject.lovable_clone.repository.PlanRepository;
import com.example.aiproject.lovable_clone.repository.UserRepository;
import com.example.aiproject.lovable_clone.service.PaymentProcessor;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProcessor implements PaymentProcessor {
    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    @Value("${client.url}")
    private String frontendUrl;

    @Override
    public CheckoutResponse createCheckoutSession(CheckoutRequest request, Long userId) {
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.planId().toString()));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user", userId.toString()));

        try {
            // 1. Check if the user already has a Stripe Customer ID
            String stripeCustomerId = user.getStripeCustomerId();

            if (stripeCustomerId == null || stripeCustomerId.isBlank()) {
                // 2. If not, create a new Customer in Stripe
                CustomerCreateParams customerParams = CustomerCreateParams.builder()
                        .setEmail(user.getUsername()) // Assuming username is their email
                        .setName(user.getName())
                        .build();

                Customer stripeCustomer = Customer.create(customerParams);
                stripeCustomerId = stripeCustomer.getId(); // Looks like "cus_R2aX..."

                // 3. Save the new Stripe ID to your PostgreSQL database
                user.setStripeCustomerId(stripeCustomerId);
                userRepository.save(user);
            }

            // 4. Build the Checkout Session
            SessionCreateParams params = SessionCreateParams.builder()
                    .setCustomer(stripeCustomerId) // <-- CRITICAL: Link session to the customer
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(plan.getStripePriceId())
                                    .setQuantity(1L)
                                    .build())
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSubscriptionData(
                            new SessionCreateParams.SubscriptionData.Builder()
                                    .setBillingMode(SessionCreateParams.SubscriptionData.BillingMode.builder()
                                            .setType(SessionCreateParams.SubscriptionData.BillingMode.Type.FLEXIBLE)
                                            .build())
                                    .build()
                    )
                    .setSuccessUrl(frontendUrl + "/success.html?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/cancel.html")
                    .putMetadata("user_id", userId.toString())
                    .putMetadata("plan_id", plan.getId().toString())
                    .build();

            // 5. Make backend API call to Stripe
            Session session = Session.create(params);
            return new CheckoutResponse(session.getUrl());

        } catch (StripeException e) {
            // Throw a more descriptive error if Stripe fails
            throw new RuntimeException("Error communicating with Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public PortalResponse openCustomerPortal(Long userId) {
        return null;
    }

    @Override
    public void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata) {
        log.info(type);
    }
}