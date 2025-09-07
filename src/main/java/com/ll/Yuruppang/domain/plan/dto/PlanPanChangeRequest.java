package com.ll.Yuruppang.domain.plan.dto;

import jakarta.validation.constraints.NotNull;

public record PlanPanChangeRequest(
        @NotNull
        Long panId
) {}
