package com.ll.Yuruppang.domain.plan.dto.detailResponse;

import java.math.BigDecimal;
import java.util.List;

public record ComparedPlanRecipeDetailDto(
    Long recipeId,
    String recipeName,
    String recipeDescription,
    BigDecimal totalPrice,
    String customRecipeName,
    String customRecipeDescription,
    boolean isTemp,
    int outputQuantity,
    int goalQuantity,
    int percent,
    List<ComparedPartDto> comparedParts
){
}
