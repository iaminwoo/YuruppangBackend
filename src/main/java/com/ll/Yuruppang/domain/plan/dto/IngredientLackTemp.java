package com.ll.Yuruppang.domain.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class IngredientLackTemp {
    private final Long ingredientId;
    private final String name;
    private BigDecimal requiredQuantity;
    private final BigDecimal currentStock;

    public void addRequiredQuantity(BigDecimal customizedQuantity) {
        this.requiredQuantity = this.requiredQuantity.add(customizedQuantity);
    }
}
