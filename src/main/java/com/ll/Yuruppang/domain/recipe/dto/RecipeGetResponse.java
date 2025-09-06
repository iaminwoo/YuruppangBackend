package com.ll.Yuruppang.domain.recipe.dto;

import com.ll.Yuruppang.domain.recipe.dto.pan.PanResponse;

import java.math.BigDecimal;
import java.util.List;

public record RecipeGetResponse(
        String name,
        String description,
        int outputQuantity,
        PanResponse pan,
        BigDecimal totalPrice,
        List<RecipePartGetDto> parts,
        String categoryName,
        Long categoryId
) { }
