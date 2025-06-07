package com.ll.Yuruppang.domain.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlanCreateRequest(
        @NotBlank(message = "메모를 입력해주세요.")
        String memo,
        @NotEmpty(message = "최소 하나 이상의 레시피가 필요합니다.")
        List<Long> recipes
) {
}
