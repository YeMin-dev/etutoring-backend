package com.a9.etutoring.service;

import com.a9.etutoring.domain.dto.report.MessagingReportResponse;

public interface MessagingReportService {

    /**
     * Rolling window of {@code windowDays} ending at {@code Instant.now()} (exclusive upper bound).
     */
    MessagingReportResponse generateMessagingReport(int windowDays);
}
