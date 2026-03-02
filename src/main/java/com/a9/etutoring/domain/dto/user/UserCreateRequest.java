package com.a9.etutoring.domain.dto.user;

import com.a9.etutoring.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 255) String password,
    @NotNull UserRole role,
    Boolean isActive,
    Boolean isLocked
) {
}
