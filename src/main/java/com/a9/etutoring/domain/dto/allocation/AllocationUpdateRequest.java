package com.a9.etutoring.domain.dto.allocation;

import java.time.Instant;
import java.util.UUID;

public record AllocationUpdateRequest(
    String reason,
    Instant scheduleStart,
    Instant scheduleEnd,
    UUID studentUserId,
    UUID tutorUserId
) {
}
