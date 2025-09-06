package com.ll.Yuruppang.domain.recipe.dto.pan;

import com.ll.Yuruppang.domain.recipe.entity.PanType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PanRequest(
        @NotNull
        PanType panType,
        BigDecimal radius,
        BigDecimal width,
        BigDecimal length,
        BigDecimal height,
        BigDecimal volume
) {}
