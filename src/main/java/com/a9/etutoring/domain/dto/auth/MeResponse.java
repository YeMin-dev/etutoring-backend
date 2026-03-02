package com.a9.etutoring.domain.dto.auth;

import com.a9.etutoring.domain.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

public record MeResponse(
    UUID id,
    String username,
    String firstName,
    String lastName,
    String email,
    UserRole role,
    boolean isActive,
    boolean isLocked,
    Instant lastLoginDate
) {
}
