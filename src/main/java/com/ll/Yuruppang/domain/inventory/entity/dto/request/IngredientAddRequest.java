package com.ll.Yuruppang.domain.inventory.entity.dto.request;

import com.ll.Yuruppang.domain.inventory.entity.IngredientUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IngredientAddRequest(
        @NotBlank String name,
        @NotNull IngredientUnit unit,
        @NotNull String totalPrice,
        @NotNull String totalQuantity
) {}
