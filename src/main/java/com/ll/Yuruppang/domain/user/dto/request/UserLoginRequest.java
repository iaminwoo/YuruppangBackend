package com.ll.Yuruppang.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @NotBlank
        String pin
) {
}
