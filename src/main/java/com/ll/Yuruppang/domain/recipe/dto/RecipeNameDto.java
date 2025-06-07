package com.ll.Yuruppang.domain.recipe.dto;

public record RecipeNameDto(
        Long recipeId,
        String recipeName,
        int outputQuantity,
        boolean favorite
) {
}
