package com.ll.Yuruppang.domain.plan.dto;

import java.util.List;

public record PlanSimpleResponse(
        Long planId,
        String planName,
        List<String> recipeNames,
        int recipeCount,
        boolean isComplete
) {
}
