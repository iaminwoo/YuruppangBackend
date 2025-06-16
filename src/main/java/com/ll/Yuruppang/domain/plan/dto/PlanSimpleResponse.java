package com.ll.Yuruppang.domain.plan.dto;

import java.time.LocalDate;
import java.util.List;

public record PlanSimpleResponse(
        Long planId,
        String planName,
        LocalDate createdAt,
        LocalDate completedAt,
        List<String> recipeNames,
        int recipeCount,
        boolean isComplete
) {
}
