package com.ll.Yuruppang.domain.recipe.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecipeGetResponse(
        String name,
        String description,
        int outputQuantity,
        BigDecimal totalPrice,
        List<RecipePartGetDto> parts,
        String categoryName,
        Long categoryId
) { }
