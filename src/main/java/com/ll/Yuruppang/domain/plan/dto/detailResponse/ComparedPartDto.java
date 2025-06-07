package com.ll.Yuruppang.domain.plan.dto.detailResponse;

import java.util.List;

public record ComparedPartDto(
        String partName,
        int percent,
        List<ComparedIngredientDto> comparedIngredients
) {
}
