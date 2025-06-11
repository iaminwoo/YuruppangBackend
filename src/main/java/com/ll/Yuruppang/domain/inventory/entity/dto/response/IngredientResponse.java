package com.ll.Yuruppang.domain.inventory.entity.dto.response;

import com.ll.Yuruppang.domain.inventory.entity.IngredientUnit;

import java.math.BigDecimal;

public record IngredientResponse(
        Long ingredientId,
        String name,
        IngredientUnit unit,
        BigDecimal unitPrice,
        BigDecimal totalStock,
        BigDecimal density
) {}
