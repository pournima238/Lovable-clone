package com.example.aiproject.lovable_clone.controller;

import com.example.aiproject.lovable_clone.dto.Subscriptions.*;
import com.example.aiproject.lovable_clone.security.AuthUtil;
import com.example.aiproject.lovable_clone.service.PaymentProcessor;
import com.example.aiproject.lovable_clone.service.PlanService;
import com.example.aiproject.lovable_clone.service.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BillingController {
    private final PlanService planService;
    private final SubscriptionService subscriptionService;
    private final AuthUtil auth;
    private final PaymentProcessor paymentProcessor;

    @Value("${stripe.webhook.secret}")
    public String webhookSecret;

    @GetMapping("/api/plans")
    public ResponseEntity<PlanResponse> getAllPlans() {
        return ResponseEntity.ok(planService.getAllActivePlans());
    }

    @GetMapping("/api/me/subscriptions")
    public ResponseEntity<SubscriptionResponse> getMySubscriptions() {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(userId));
    }

    @PostMapping("/api/payments/checkout")
    public ResponseEntity<CheckoutResponse> createCheckoutResponse(
            @RequestBody CheckoutRequest request
    ) {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(paymentProcessor.createCheckoutSession(request, userId));
    }

    @PostMapping("/api/payments/portal")
    public ResponseEntity<PortalResponse> openCustomerPortal() {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(paymentProcessor.openCustomerPortal(userId));
    }

    @PostMapping("/webhooks/payment")
    public ResponseEntity<String> handlePaymentWebHooks(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        try {
            log.info("🔔 Webhook received! Verifying signature...");
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            // 2. Deserialize the underlying Stripe object
            StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);

            if (stripeObject == null) {
                // CHANGED: Log a warning and return 200 OK so Stripe stops retrying
                log.warn("⚠️ Failed to deserialize Stripe object for event type: {}. This is usually a version mismatch between the CLI and the Java SDK.", event.getType());
                return ResponseEntity.ok("Webhook received, but object could not be deserialized.");
            }

            // 3. Extract metadata based on the specific Stripe Object type
            Map<String, String> metadata = new HashMap<>();

            if (stripeObject instanceof Session session) {
                metadata = session.getMetadata();
            } else if (stripeObject instanceof Subscription subscription) {
                metadata = subscription.getMetadata();
            } else if (stripeObject instanceof Customer customer) {
                metadata = customer.getMetadata();
            } else if (stripeObject instanceof PaymentIntent paymentIntent) {
                metadata = paymentIntent.getMetadata();
            }

            if (metadata == null) {
                metadata = new HashMap<>();
            }

            log.info("🔄 Extracted metadata: {}", metadata);

            // 4. Pass the extracted data to your service layer
            paymentProcessor.handleWebhookEvent(event.getType(), stripeObject, metadata);
            // 5. Always return a 200 OK to Stripe
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (SignatureVerificationException e) {
            log.error("❌ Invalid Stripe signature!", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        } catch (Exception e) {
            log.error("❌ General Webhook error!", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error: " + e.getMessage());
        }
    }
}