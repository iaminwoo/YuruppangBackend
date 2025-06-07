package com.ll.Yuruppang.domain.recipe.dto.category;

import java.util.List;

public record DeleteCategoryRequest(
        List<DeleteCategoryRecipesDto> recipes
) {
}
