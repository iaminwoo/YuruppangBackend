package com.ll.Yuruppang.domain.plan.dto.detailResponse;

import com.ll.Yuruppang.domain.inventory.entity.IngredientUnit;

import java.math.BigDecimal;

public record ComparedIngredientDto(
        Long ingredientId,
        Long ingredientPartId,
        String ingredientName,
        IngredientUnit unit,
        BigDecimal originalQuantity,
        BigDecimal customizedQuantity
){
}
