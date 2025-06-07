package com.ll.Yuruppang.domain.plan.repository;

import com.ll.Yuruppang.domain.plan.entity.BakingPlanRecipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRecipeRepository extends JpaRepository<BakingPlanRecipe, Long> {
}
