package com.ll.Yuruppang.domain.inventory.entity.dto.request;

import jakarta.validation.constraints.Min;

public record EggBreakRequest(
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        int quantity
) {}

