package com.ll.Yuruppang.domain.recipe.dto.pan;

import com.ll.Yuruppang.domain.recipe.entity.PanType;

import java.math.BigDecimal;

public record PanResponse(
        Long panId,
        PanType panType,
        String measurements,
        BigDecimal volume
) {}
