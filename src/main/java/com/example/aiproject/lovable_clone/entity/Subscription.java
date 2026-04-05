package com.example.aiproject.lovable_clone.entity;

import com.example.aiproject.lovable_clone.enums.SubscriptionStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@FieldDefaults(level= AccessLevel.PRIVATE)
//after writing this no need to write private for all attributes
@Getter
@Setter
public class Subscription {
    Long id;
    User user;
    Plan plan;
    String stripeCustomerId;
    String stripeSubscriptionId;
    //good thing about this is that now database will enforce unique constraint for this
    SubscriptionStatus status;
    Instant currentPeriodStart;
    Instant currentPeriodEnd;
    Boolean cancelPeriodEnd;
    Instant createdAt;
    Instant updatedAt;

}
