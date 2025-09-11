package com.ll.Yuruppang.domain.recipe.dto;

import com.ll.Yuruppang.domain.inventory.entity.IngredientUnit;

import java.math.BigDecimal;

public record RecipeIngredientGetDto(
        Long ingredientId,
        Long ingredientPartId,
        String ingredientName,
        BigDecimal requiredQuantity,
        Integer orderIndex,
        IngredientUnit unit,
        BigDecimal stockQuantity
) {
}
