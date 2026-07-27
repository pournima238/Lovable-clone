package com.example.intelligence_service.client;

import com.example.common_lib.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service", path = "/account")
public interface AccountClient {

    @GetMapping("/internal/v1/users/{id}")
    UserDto getUser(@PathVariable("id") Long id);

    @GetMapping("/api/users/{userId}/subscription")
    SubscriptionResponse getSubscription(@PathVariable("userId") Long userId);

    record UserSummaryResponse(Long id, String email) {}
    record SubscriptionResponse(Long id, String status, PlanResponse plan) {}
    record PlanResponse(boolean unlimitedAi, int maxTokensPerDay) {}
}
