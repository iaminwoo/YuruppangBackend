package com.ll.Yuruppang.domain.inventory.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IngredientUseRequest(
        @NotBlank String name,
        @NotNull String totalQuantity
) {}
