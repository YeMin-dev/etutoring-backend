package com.a9.etutoring.domain.dto.user;

import com.a9.etutoring.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @Size(min = 3, max = 50) String username,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Email @Size(max = 255) String email,
    @Size(min = 8, max = 255) String password,
    UserRole role,
    Boolean isActive,
    Boolean isLocked
) {
}
