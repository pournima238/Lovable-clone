package com.example.account_service.services.impl;

import com.example.account_service.dto.Subscriptions.CheckoutRequest;
import com.example.account_service.dto.Subscriptions.CheckoutResponse;
import com.example.account_service.dto.Subscriptions.PortalResponse;
import com.example.account_service.entity.Plan;
import com.example.account_service.entity.User;
import com.example.account_service.repository.PlanRepository;
import com.example.account_service.repository.UserRepository;
import com.example.account_service.services.PaymentProcessor;
import com.example.account_service.services.SubscriptionService;
import com.example.common_lib.enums.SubscriptionStatus;
import com.example.common_lib.error.BadRequestException;
import com.example.common_lib.error.ResourceNotFoundException;
import com.example.common_lib.security.AuthUtil;
import com.stripe.exception.StripeException;

import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProcessor implements PaymentProcessor {
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final AuthUtil authUtil;

    @Value("${client.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public CheckoutResponse createCheckoutSession(CheckoutRequest request, Long userId) {
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.planId().toString()));
        User user = getUser(userId);

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
        User user = getUser(userId);
        String stripeCustomerId = user.getStripeCustomerId();
        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            throw new BadRequestException("User does not have a stripe customer id,UserId" + userId);
        }
        try {
            var portalSession = com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(stripeCustomerId)
                            .setReturnUrl(frontendUrl)
                            .build()
            );
            return new PortalResponse(portalSession.getUrl());
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata) {
        log.debug("Handling stripe event: {}", type);
        switch (type) {
            case "checkout.session.completed" ->
                    handleCheckoutSessionCompleted((Session) stripeObject, metadata); //one time when checkout is completed
            case "customer.subscription.updated" ->
                    handleCustomerSubscriptionUpdated((Subscription) stripeObject);//when user upgrades its subscription
            case "customer.subscription.deleted" ->
                    handleCustomerSubscriptionDeleted((Subscription) stripeObject);// when subscription ends, revoke the access
            case "invoice.paid" -> handleInvoicePaid((Invoice) stripeObject); //when invoice is paid
            case "invoice.payment_failed" ->
                    handleInvoicePaymentFailed((Invoice) stripeObject); //when invoice is not paid mark it as failed
            default -> log.debug("Ignoring the event {}", type);
        }
    }

    private void handleCheckoutSessionCompleted(Session session, Map<String, String> metadata) {
        if (session == null) {
            log.error("session object was null");
            return;
        }
        Long userId = Long.parseLong(metadata.get("user_id"));
        Long planId = Long.parseLong(metadata.get("plan_id"));
        String subscriptionId = session.getSubscription();
        String customerId = session.getCustomer();
        User user = getUser(userId);
        if (user.getStripeCustomerId() == null) {
            user.setStripeCustomerId(customerId);
            userRepository.save(user);
        }
        subscriptionService.activateSubscription(userId, planId, subscriptionId, customerId);
    }

    private void handleCustomerSubscriptionUpdated(Subscription subscription) {
        if (subscription == null) {
            log.error("subscription object was null");
            return;
        }
        SubscriptionStatus status = mapStripeStatusToEnum(subscription.getStatus());
        if (status == null) {
            log.warn("Unknown status '{}' for subscription {}", subscription.getStatus(), subscription.getId());
            return;
        }

        SubscriptionItem item = subscription.getItems().getData().get(0);
        Instant periodStart = toInstant(item.getCurrentPeriodStart());
        Instant periodEnd = toInstant(item.getCurrentPeriodEnd());

        Long planId = resolvePlanId(item.getPrice());
        subscriptionService.updateSubscription(subscription.getId(), status, periodStart, periodEnd,
                subscription.getCancelAtPeriodEnd(), planId);
    }

    private SubscriptionStatus mapStripeStatusToEnum(String status) {
        return switch (status) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due", "unpaid", "paused", "incomplete_expired" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELLED;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            default -> {
                log.warn("Unmapped Stripe status: {}", status);
                yield null;
            }
        };
    }

    private void handleCustomerSubscriptionDeleted(Subscription subscription) {
        if (subscription == null) {
            log.error("subscription object was null");
            return;
        }
        subscriptionService.cancelSubscription(subscription.getId());
    }

    private void handleInvoicePaid(Invoice invoice) {
        if (invoice == null) {
            log.error("invoice object was null");
            return;
        }
        String subId = extractSubscription(invoice);
        if (subId == null) return;
        try {
            Subscription subscription = Subscription.retrieve(subId); //sdk calling the stripe server
            SubscriptionItem item = subscription.getItems().getData().get(0);
            Instant periodStart = toInstant(item.getCurrentPeriodStart());
            Instant periodEnd = toInstant(item.getCurrentPeriodEnd());
            subscriptionService.renewSubscription(subId, periodStart, periodEnd);
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleInvoicePaymentFailed(Invoice invoice) {
        if (invoice == null) {
            log.error("invoice object was null");
            return;
        }
        String subId = extractSubscription(invoice);
        if (subId == null) return;
        subscriptionService.markSubscriptionPastDue(subId);
    }

    //Utility methods
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user", userId.toString()));
    }

    private Instant toInstant(Long epoch) {
        return epoch != null ? Instant.ofEpochSecond(epoch) : null;
    }

    private Long resolvePlanId(Price price) {
        if (price == null || price.getId() == null) return null;
        return planRepository.findPlanByStripePriceId(price.getId())
                .map(Plan::getId).orElse(null);
    }

    private String extractSubscription(Invoice invoice) {
        var parent = invoice.getParent();
        if (parent == null) return null;
        var subDetails = parent.getSubscriptionDetails();
        if (subDetails == null) return null;
        return subDetails.getSubscription();
    }
}