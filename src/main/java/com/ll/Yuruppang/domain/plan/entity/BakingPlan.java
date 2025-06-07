package com.ll.Yuruppang.domain.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BakingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Lob
    private String memo;

    @Builder.Default
    private boolean isComplete = false;

    @Builder.Default
    @OneToMany(mappedBy = "bakingPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BakingPlanRecipe> recipes = new HashSet<>();

    public void complete() {
        this.isComplete = true;
    }

    public void addRecipe(BakingPlanRecipe bakingPlanRecipe) {
        this.recipes.add(bakingPlanRecipe);
        bakingPlanRecipe.setBakingPlan(this);
    }

    public void deleteRecipe(BakingPlanRecipe bakingPlanRecipe) {
        this.recipes.remove(bakingPlanRecipe);
    }

    public void changeMemo(String newMemo) {
        this.memo = newMemo;
    }
}
