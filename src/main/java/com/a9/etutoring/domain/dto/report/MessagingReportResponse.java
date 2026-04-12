package com.a9.etutoring.domain.dto.report;

import java.time.Instant;
import java.util.List;

public record MessagingReportResponse(
    Instant windowStart,
    Instant windowEndExclusive,
    int windowDays,
    long totalMessagesInWindow,
    List<TutorMessagingStatsRow> tutors
) {
}
