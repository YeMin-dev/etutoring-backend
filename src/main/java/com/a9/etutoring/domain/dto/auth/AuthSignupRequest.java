package com.a9.etutoring.domain.dto.auth;

import com.a9.etutoring.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthSignupRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 255) String password,
    UserRole role
) {
}
