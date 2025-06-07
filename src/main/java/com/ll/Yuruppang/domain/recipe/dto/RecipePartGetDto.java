package com.ll.Yuruppang.domain.recipe.dto;

import java.util.List;

public record RecipePartGetDto(
        String partName,
        List<RecipeIngredientGetDto> ingredients
) {
}
