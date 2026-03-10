package com.a9.etutoring.domain.dto.allocation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record AllocationPreviewRequest(
    @NotNull LocalDate date,
    @NotNull @Positive Integer slotDurationMinutes,
    @NotNull UUID tutorUserId,
    @NotEmpty @Size(max = 500) List<UUID> studentUserIds,
    String reason,
    String timeZoneId,
    LocalTime startTime
) {
}
