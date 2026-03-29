package com.a9.etutoring.domain.dto.assignment;

import java.time.Instant;
import java.util.UUID;

public record AssignmentSummaryResponse(
    UUID id,
    UUID createdById,
    String title,
    Instant dueDate,
    Instant createdDate,
    Instant updatedDate
) {}
