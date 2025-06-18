package com.ll.Yuruppang.domain.inventory.entity.dto.response;

import java.math.BigDecimal;

public record EggResponse(
        BigDecimal eggsCount,
        BigDecimal whitesStock,
        BigDecimal yolksStock
) {
}
