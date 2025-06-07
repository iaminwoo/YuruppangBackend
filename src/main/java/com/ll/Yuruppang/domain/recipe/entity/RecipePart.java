package com.ll.Yuruppang.domain.recipe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipePart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    private int percent = 100;

    @OneToMany(mappedBy = "recipePart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RecipePartIngredient> ingredients = new HashSet<>();

    public void addIngredient(RecipePartIngredient ingredient) {
        this.ingredients.add(ingredient);
        ingredient.setRecipePart(this);
    }
}
