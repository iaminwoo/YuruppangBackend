package com.ll.Yuruppang.domain.plan.dto.complete;

import java.util.List;

public record PlanCompleteRequest(
        List<PlanRecipeCompleteDto> recipes
) {
}
