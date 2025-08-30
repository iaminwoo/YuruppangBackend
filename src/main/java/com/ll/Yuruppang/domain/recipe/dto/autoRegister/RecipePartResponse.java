package com.ll.Yuruppang.domain.recipe.dto.autoRegister;

import java.util.List;

public record RecipePartResponse(
        String partName,
        List<RecipeIngredientResponse> ingredients
) {}
