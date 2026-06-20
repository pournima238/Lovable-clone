package com.example.aiproject.lovable_clone.mapper;

import com.example.aiproject.lovable_clone.dto.Subscriptions.PlanResponse;
import com.example.aiproject.lovable_clone.dto.Subscriptions.SubscriptionResponse;
import com.example.aiproject.lovable_clone.entity.Plan;
import com.example.aiproject.lovable_clone.entity.Subscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanResponse toPlanResponse(Plan plan);
}
