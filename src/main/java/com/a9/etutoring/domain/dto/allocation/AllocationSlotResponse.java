package com.a9.etutoring.domain.dto.allocation;

import java.time.Instant;

public record AllocationSlotResponse(
    Instant scheduleStart,
    Instant scheduleEnd
) {
}
