package com.ll.Yuruppang.domain.recipe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record RecipePartDto(
        @NotBlank(message = "파트 이름은 필수입니다.")
        String partName,

        @NotEmpty(message = "최소 하나 이상의 재료가 필요합니다.")
        @Valid
        List<RecipeIngredientDto> ingredients
) { }