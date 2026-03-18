package com.a9.etutoring.domain.dto.meeting;

import com.a9.etutoring.domain.enums.MeetingMode;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record MeetingCreateRequest(
    @NotNull UUID studentUserId,
    @NotNull Instant startDate,
    @NotNull Instant endDate,
    @NotNull MeetingMode mode,
    String location,
    String link,
    String description
) {
}
