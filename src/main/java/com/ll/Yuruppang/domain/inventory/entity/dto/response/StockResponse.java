package com.ll.Yuruppang.domain.inventory.entity.dto.response;

import com.ll.Yuruppang.domain.inventory.entity.dto.IngredientDto;

import java.util.List;

public record StockResponse(
        List<IngredientDto> ingredients,
        int totalPage
) {
}
