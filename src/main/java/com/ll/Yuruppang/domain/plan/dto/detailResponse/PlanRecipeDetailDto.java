package com.ll.Yuruppang.domain.plan.dto.detailResponse;

import java.math.BigDecimal;
import java.util.List;

public record PlanRecipeDetailDto(
        Long recipeId,
        String name,
        String description,
        boolean isTemp,
        int outputQuantity,
        int goalQuantity,
        List<PlanIngredientDto> ingredients,
        BigDecimal unitProductionCost
) {
}
