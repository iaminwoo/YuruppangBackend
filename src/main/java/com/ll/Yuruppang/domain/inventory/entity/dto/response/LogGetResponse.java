package com.ll.Yuruppang.domain.inventory.entity.dto.response;

import com.ll.Yuruppang.domain.inventory.entity.LogType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LogGetResponse(
        Long id,
        LocalDate actualAt,
        LogType type,
        String description,
        Long ingredientId,
        String ingredientName,
        BigDecimal quantity,
        String unit,
        BigDecimal totalPrice
) {
}
