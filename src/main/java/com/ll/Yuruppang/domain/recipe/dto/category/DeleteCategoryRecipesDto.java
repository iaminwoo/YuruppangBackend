package com.ll.Yuruppang.domain.recipe.dto.category;

public record DeleteCategoryRecipesDto(
        Long recipeId,
        Long newCategoryId
) {
}
