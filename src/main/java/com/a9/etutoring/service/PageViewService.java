package com.a9.etutoring.service;

import com.a9.etutoring.domain.dto.analytics.PageViewRequest;
import com.a9.etutoring.domain.dto.analytics.UsageSummaryResponse;
import java.time.LocalDate;
import java.util.UUID;

public interface PageViewService {

    void recordPageView(UUID userId, PageViewRequest request, String userAgent);

    UsageSummaryResponse getUsageSummary(LocalDate from, LocalDate to);
}
