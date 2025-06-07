package com.ll.Yuruppang.domain.recipe.dto;

import com.ll.Yuruppang.domain.inventory.entity.IngredientUnit;

import java.math.BigDecimal;

public record RecipeIngredientGetDto(
        Long ingredientId,
        String ingredientName,
        BigDecimal requiredQuantity,
        IngredientUnit unit,
        BigDecimal stockQuantity
) {
}
