package com.ll.Yuruppang.domain.recipe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record RecipeCreateRequest(
        @NotBlank(message = "레시피 이름은 필수입니다.")
        String name,
        @Size(max = 500, message = "설명은 최대 500자까지 가능합니다.")
        String description,
        @NotNull(message = "생산 수량은 필수입니다.")
        @Min(value = 1, message = "생산 수량은 1 이상이어야 합니다.")
        int outputQuantity,
        @NotEmpty(message = "최소 하나 이상의 파트가 필요합니다.")
        @Valid
        List<RecipePartDto> parts,
        @NotNull(message = "카테고리는 필수 입니다.")
        Long categoryId
) { }
