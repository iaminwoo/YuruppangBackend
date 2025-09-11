package com.ll.Yuruppang.domain.recipe.entity;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipePartIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_part_id")
    private RecipePart recipePart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    private Integer orderIndex;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    public void multiply(int oldOutput, int newGoalQuantity) {
        BigDecimal ratio = (new BigDecimal(newGoalQuantity)).divide(new BigDecimal(oldOutput), 2, RoundingMode.HALF_UP);
        this.quantity = quantity.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }
}
