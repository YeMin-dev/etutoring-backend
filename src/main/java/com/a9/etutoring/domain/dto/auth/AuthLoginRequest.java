package com.a9.etutoring.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {
}
