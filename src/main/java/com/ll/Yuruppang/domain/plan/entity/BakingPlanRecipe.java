package com.ll.Yuruppang.domain.plan.entity;

import com.ll.Yuruppang.domain.recipe.entity.Recipe;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BakingPlanRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baking_plan_id")
    private BakingPlan bakingPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_recipe_id")
    private Recipe originalRecipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customized_recipe_id")
    private Recipe customizedRecipe;
}
