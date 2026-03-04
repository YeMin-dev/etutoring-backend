package com.a9.etutoring.domain.dto.allocation;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AllocationCreateRequest(
    @NotNull UUID studentUserId,
    @NotNull UUID tutorUserId,
    String reason,
    @NotNull Instant scheduleStart,
    @NotNull Instant scheduleEnd
) {
}
