package com.a9.etutoring.domain.dto.analytics;

import java.util.List;

public record UsageSummaryResponse(
    List<PageTopItem> topPages,
    List<UserActivityItem> topUsers,
    List<BrowserCountItem> browsers
) {}
