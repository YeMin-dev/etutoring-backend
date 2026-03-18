package com.a9.etutoring.domain.dto.allocation;

import java.time.Instant;
import java.util.UUID;

public record TutorAllocationResponse(
    UUID id,
    UUID studentUserId,
    UUID tutorUserId,
    UUID allocatedById,
    Instant allocatedDate,
    Instant endedDate,
    String reason,
    Instant scheduleStart,
    Instant scheduleEnd
) {
}
