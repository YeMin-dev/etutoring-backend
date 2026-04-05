package com.a9.etutoring.domain.dto.meeting;

import com.a9.etutoring.domain.enums.MeetingMode;
import com.a9.etutoring.domain.enums.VirtualMeetingPlatform;
import java.time.Instant;

public record MeetingUpdateRequest(
    Instant startDate,
    Instant endDate,
    MeetingMode mode,
    String location,
    String link,
    VirtualMeetingPlatform virtualPlatform,
    String description
) {
}
