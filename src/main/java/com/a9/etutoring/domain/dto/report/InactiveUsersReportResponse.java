package com.a9.etutoring.domain.dto.report;

import java.time.Instant;
import java.util.List;

public record InactiveUsersReportResponse(
    int daysThreshold,
    Instant generatedAt,
    List<InactiveUserResponse> students,
    List<InactiveUserResponse> tutors
) {
}
