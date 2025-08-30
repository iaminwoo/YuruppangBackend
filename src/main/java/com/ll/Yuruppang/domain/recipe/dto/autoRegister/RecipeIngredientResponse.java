package com.ll.Yuruppang.domain.recipe.dto.autoRegister;

import com.ll.Yuruppang.domain.inventory.entity.IngredientUnit;

import java.math.BigDecimal;

public record RecipeIngredientResponse(
        String ingredientName,
        BigDecimal quantity,
        IngredientUnit unit,
        BigDecimal stock
) {
}
