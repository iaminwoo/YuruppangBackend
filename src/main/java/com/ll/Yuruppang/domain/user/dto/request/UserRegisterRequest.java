package com.ll.Yuruppang.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserRegisterRequest(
        @NotBlank
        String username,
        @NotBlank
        String pin
) {
}
