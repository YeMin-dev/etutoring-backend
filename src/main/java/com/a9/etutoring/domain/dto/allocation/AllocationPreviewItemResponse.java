package com.a9.etutoring.domain.dto.allocation;

import java.util.UUID;

public record AllocationPreviewItemResponse(
    UUID studentUserId,
    UUID tutorUserId,
    String reason,
    String scheduleStart,
    String scheduleEnd
) {
}
