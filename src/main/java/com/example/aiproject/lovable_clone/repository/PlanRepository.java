package com.example.aiproject.lovable_clone.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.example.aiproject.lovable_clone.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findPlanByStripePriceId(String id);
}
