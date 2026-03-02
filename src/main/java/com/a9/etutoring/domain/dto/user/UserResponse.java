package com.a9.etutoring.domain.dto.user;

import com.a9.etutoring.domain.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    UserRole role,
    String username,
    String firstName,
    String lastName,
    String email,
    Boolean isActive,
    Boolean isLocked,
    Instant createdDate,
    Instant updatedDate,
    Instant lastLoginDate
) {
}
