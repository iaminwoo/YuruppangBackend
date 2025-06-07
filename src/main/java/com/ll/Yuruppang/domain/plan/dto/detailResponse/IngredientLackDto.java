package com.ll.Yuruppang.domain.plan.dto.detailResponse;

import java.math.BigDecimal;

public record IngredientLackDto(
        Long ingredientId,
        String name,
        BigDecimal requiredQuantity,
        BigDecimal currentStock,
        BigDecimal lackingQuantity
) {}
