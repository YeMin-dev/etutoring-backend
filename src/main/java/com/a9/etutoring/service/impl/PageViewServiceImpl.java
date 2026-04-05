package com.a9.etutoring.service.impl;

import com.a9.etutoring.config.AppProperties;
import com.a9.etutoring.domain.dto.analytics.BrowserCountItem;
import com.a9.etutoring.domain.dto.analytics.PageTopItem;
import com.a9.etutoring.domain.dto.analytics.PageViewRequest;
import com.a9.etutoring.domain.dto.analytics.UsageSummaryResponse;
import com.a9.etutoring.domain.dto.analytics.UserActivityItem;
import com.a9.etutoring.domain.model.PageView;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.exception.ResourceNotFoundException;
import com.a9.etutoring.repository.PageViewRepository;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.service.PageViewService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PageViewServiceImpl implements PageViewService {

    private static final int MAX_RANGE_DAYS = 366;

    private final PageViewRepository pageViewRepository;
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    public PageViewServiceImpl(
        PageViewRepository pageViewRepository,
        UserRepository userRepository,
        AppProperties appProperties
    ) {
        this.pageViewRepository = pageViewRepository;
        this.userRepository = userRepository;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public void recordPageView(UUID userId, PageViewRequest request, String userAgent) {
        User user = userRepository.findByIdAndDeletedDateIsNull(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
        String path = normalizePagePath(request.pagePath());
        PageView row = PageView.builder()
            .id(UUID.randomUUID())
            .viewedAt(Instant.now())
            .pagePath(path)
            .user(user)
            .browser(parseBrowserLabel(userAgent))
            .build();
        pageViewRepository.save(row);
    }

    @Override
    @Transactional(readOnly = true)
    public UsageSummaryResponse getUsageSummary(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("INVALID_RANGE", "from and to dates are required");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("INVALID_RANGE", "to must be on or after from");
        }
        long spanDays = ChronoUnit.DAYS.between(from, to);
        if (spanDays > MAX_RANGE_DAYS - 1) {
            throw new BadRequestException("INVALID_RANGE", "Date range must be at most " + MAX_RANGE_DAYS + " inclusive days");
        }
        ZoneId zone = appProperties.getDefaultTimeZone();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant endExclusive = to.plusDays(1).atStartOfDay(zone).toInstant();

        List<PageTopItem> topPages = new ArrayList<>();
        for (Object[] row : pageViewRepository.aggregateTopPages(start, endExclusive)) {
            topPages.add(new PageTopItem((String) row[0], toLong(row[1])));
        }

        List<UserActivityItem> topUsers = new ArrayList<>();
        for (Object[] row : pageViewRepository.aggregateTopUsers(start, endExclusive)) {
            topUsers.add(new UserActivityItem(toUuid(row[0]), (String) row[1], (String) row[2], toLong(row[3])));
        }

        List<BrowserCountItem> browsers = new ArrayList<>();
        for (Object[] row : pageViewRepository.aggregateBrowsers(start, endExclusive)) {
            browsers.add(new BrowserCountItem((String) row[0], toLong(row[1])));
        }

        return new UsageSummaryResponse(topPages, topUsers, browsers);
    }

    private static long toLong(Object o) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        throw new IllegalStateException("Unexpected count type: " + (o == null ? "null" : o.getClass()));
    }

    private static UUID toUuid(Object o) {
        if (o instanceof UUID u) {
            return u;
        }
        return UUID.fromString(o.toString());
    }

    static String parseBrowserLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Other";
        }
        String u = userAgent.toLowerCase();
        if (u.contains("edg/") || u.contains("edg ")) {
            return "Edge";
        }
        if (u.contains("chrome") && !u.contains("edg")) {
            return "Chrome";
        }
        if (u.contains("firefox")) {
            return "Firefox";
        }
        if (u.contains("safari") && !u.contains("chrome")) {
            return "Safari";
        }
        return "Other";
    }

    private static String normalizePagePath(String raw) {
        String t = raw.trim();
        if (t.isEmpty()) {
            throw new BadRequestException("INVALID_PAGE_PATH", "pagePath cannot be blank");
        }
        if (t.length() > 255) {
            throw new BadRequestException("INVALID_PAGE_PATH", "pagePath must be at most 255 characters");
        }
        return t.startsWith("/") ? t : "/" + t;
    }
}
