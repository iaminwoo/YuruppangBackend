package com.ll.Yuruppang.domain.recipe.dto.autoRegister;

import java.util.List;

public record RecipeAutoRegisterResponse(
        String name,
        String description,
        int outputQuantity,
        Long categoryId,
        List<RecipePartResponse> parts
){}