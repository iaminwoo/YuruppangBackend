package com.ll.Yuruppang.domain.recipe.dto;

import java.util.List;

public record RecipePartGetDto(
        Long partId,
        String partName,
        List<RecipeIngredientGetDto> ingredients
) {
}
