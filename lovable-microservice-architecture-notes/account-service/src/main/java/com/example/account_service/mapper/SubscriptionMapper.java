package com.example.account_service.mapper;


import com.example.account_service.dto.Subscriptions.PlanResponse;
import com.example.account_service.dto.Subscriptions.SubscriptionResponse;
import com.example.account_service.entity.Plan;
import com.example.account_service.entity.Subscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanResponse toPlanResponse(Plan plan);
}
