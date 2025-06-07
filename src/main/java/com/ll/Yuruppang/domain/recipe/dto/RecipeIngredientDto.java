package com.ll.Yuruppang.domain.recipe.dto;

import com.ll.Yuruppang.domain.inventory.entity.IngredientUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecipeIngredientDto(
        @NotBlank(message = "재료 이름은 필수입니다.")
        String ingredientName,

        @NotNull(message = "수량은 필수입니다.")
        @DecimalMin(value = "0.01", message = "수량은 0보다 커야 합니다.")
        @Digits(integer = 8, fraction = 2, message = "정수 8자리, 소수 2자리까지 입력 가능합니다.")
        BigDecimal quantity,

        @NotNull
        IngredientUnit unit
) {
}
