package com.a9.etutoring.domain.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record TutorMessagingStatsRow(
    UUID tutorUserId,
    String username,
    String email,
    String firstName,
    String lastName,
    long messageCount,
    BigDecimal averageMessagesPerDay
) {
}
