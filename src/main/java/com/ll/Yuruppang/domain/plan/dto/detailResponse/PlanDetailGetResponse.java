package com.ll.Yuruppang.domain.plan.dto.detailResponse;

import java.util.List;

public record PlanDetailGetResponse(
        String name,
        String memo,
        boolean isComplete,
        List<ComparedPlanRecipeDetailDto> recipeDetails,
        List<IngredientLackDto> lackIngredients
) {
}
