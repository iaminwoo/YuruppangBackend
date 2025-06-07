package com.ll.Yuruppang.domain.inventory.entity.dto.request;

import com.ll.Yuruppang.domain.inventory.entity.LogType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LogModifyRequest(
        LogType type,
        String description,
        String ingredientName,
        BigDecimal quantity,
        BigDecimal price,
        LocalDate actualAt
) {
}
