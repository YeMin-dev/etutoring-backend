package com.a9.etutoring.domain.dto.report;

import com.a9.etutoring.domain.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

public record InactiveUserResponse(
    UUID userId,
    UserRole role,
    String username,
    String firstName,
    String lastName,
    String email,
    Instant lastInteractionDate,
    long inactivityDays
) {
}
