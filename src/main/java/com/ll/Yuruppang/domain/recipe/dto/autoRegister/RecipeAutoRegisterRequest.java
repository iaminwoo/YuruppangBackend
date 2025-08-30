package com.ll.Yuruppang.domain.recipe.dto.autoRegister;

import jakarta.validation.constraints.NotBlank;

public record RecipeAutoRegisterRequest(
        @NotBlank(message = "text 는 필수입니다.")
        String text
){}