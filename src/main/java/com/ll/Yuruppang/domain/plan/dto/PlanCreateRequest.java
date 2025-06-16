package com.ll.Yuruppang.domain.plan.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlanCreateRequest(
        @NotEmpty(message = "최소 하나 이상의 레시피가 필요합니다.")
        List<Long> recipes
) {
}
