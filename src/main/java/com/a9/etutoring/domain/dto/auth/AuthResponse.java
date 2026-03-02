package com.a9.etutoring.domain.dto.auth;

import com.a9.etutoring.domain.enums.UserRole;
import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    UUID id,
    String username,
    UserRole role
) {
}
