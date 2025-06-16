package com.ll.Yuruppang.domain.plan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.LinkedHashSet;
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

    private LocalDate createdAt;

    private LocalDate completedAt;

    @Builder.Default
    @OneToMany(mappedBy = "bakingPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BakingPlanRecipe> recipes = new LinkedHashSet<>();

    public void complete(String planName) {
        this.name = planName;
        this.isComplete = true;
        this.completedAt = LocalDate.now();
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
