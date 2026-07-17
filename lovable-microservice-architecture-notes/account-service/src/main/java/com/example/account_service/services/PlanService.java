package com.example.account_service.services;


import com.example.account_service.dto.Subscriptions.PlanResponse;

public interface PlanService {
    PlanResponse getAllActivePlans();
}
