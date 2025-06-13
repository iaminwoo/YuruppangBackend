package com.ll.Yuruppang.domain.plan.dto.detailResponse;

import java.util.List;

public record ComparedPartDto(
        Long partId,
        String partName,
        int percent,
        List<ComparedIngredientDto> comparedIngredients
) {
}
