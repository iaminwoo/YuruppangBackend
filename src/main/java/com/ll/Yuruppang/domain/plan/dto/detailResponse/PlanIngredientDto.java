package com.ll.Yuruppang.domain.plan.dto.detailResponse;

import com.ll.Yuruppang.domain.inventory.entity.IngredientUnit;

import java.math.BigDecimal;

public record PlanIngredientDto(
        Long ingredientId,
        String ingredientName,
        BigDecimal requiredQuantity,
        BigDecimal calculatedQuantity,
        BigDecimal stockQuantity,
        IngredientUnit unit
) {
}
