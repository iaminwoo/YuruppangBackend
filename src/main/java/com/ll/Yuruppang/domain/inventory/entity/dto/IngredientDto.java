package com.ll.Yuruppang.domain.inventory.entity.dto;

public record IngredientDto(
        Long ingredientId,
        String ingredientName,
        String unit,
        String unitPrice,
        String totalQuantity
) {
}
