package com.ll.Yuruppang.domain.recipe.repository;

import com.ll.Yuruppang.domain.inventory.entity.Ingredient;
import com.ll.Yuruppang.domain.recipe.entity.RecipePartIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartIngredientRepository extends JpaRepository<RecipePartIngredient, Long> {
    boolean existsByIngredient(Ingredient ingredient);
}
