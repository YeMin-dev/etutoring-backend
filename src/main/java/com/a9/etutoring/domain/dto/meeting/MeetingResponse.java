package com.a9.etutoring.domain.dto.meeting;

import com.a9.etutoring.domain.enums.MeetingMode;
import com.a9.etutoring.domain.enums.VirtualMeetingPlatform;
import java.time.Instant;
import java.util.UUID;

public record MeetingResponse(
    UUID id,
    UUID studentUserId,
    UUID tutorUserId,
    UUID createdById,
    Instant startDate,
    Instant endDate,
    MeetingMode mode,
    String location,
    String link,
    VirtualMeetingPlatform virtualPlatform,
    String description,
    Instant createdDate,
    Instant updatedDate
) {
}
