package com.example.aiproject.lovable_clone.controller;

import com.example.aiproject.lovable_clone.dto.Subscriptions.*;
import com.example.aiproject.lovable_clone.security.AuthUtil;
import com.example.aiproject.lovable_clone.service.PlanService;
import com.example.aiproject.lovable_clone.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BillingController {
    private final PlanService planService;
    private final SubscriptionService subscriptionService;
    private AuthUtil auth;

    @GetMapping("/api/plans")
    public ResponseEntity<PlanResponse> getAllPlans() {
        return ResponseEntity.ok(planService.getAllActivePlans());
    }

    @GetMapping("/api/me/subscriptions")
    public ResponseEntity<SubscriptionResponse> getMySubscriptions() {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(userId));
    }

    @PostMapping("/api/stripe/checkout")
    public ResponseEntity<CheckoutResponse> createCheckoutResponse(
            @RequestBody CheckoutRequest request
    ) {
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(subscriptionService.createCheckoutSession(request, userId));
    }

    @PostMapping("/api/stripe/portal")
    public  ResponseEntity<PortalResponse> openCustomerPortal(){
        Long userId = auth.getCurrentUserId();
        return ResponseEntity.ok(subscriptionService.openCustomerPortal(userId));
    }

}
