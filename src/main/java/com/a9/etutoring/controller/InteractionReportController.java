package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.analytics.UsageSummaryResponse;
import com.a9.etutoring.domain.dto.report.InactiveUsersReportResponse;
import com.a9.etutoring.domain.dto.report.MessagingReportResponse;
import com.a9.etutoring.domain.dto.user.UserResponse;
import com.a9.etutoring.service.InteractionReportService;
import com.a9.etutoring.service.MessagingReportService;
import com.a9.etutoring.service.PageViewService;
import com.a9.etutoring.service.UserService;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
public class InteractionReportController {

    private static final int MAX_PAGE_SIZE = 100;

    private final InteractionReportService interactionReportService;
    private final PageViewService pageViewService;
    private final MessagingReportService messagingReportService;
    private final UserService userService;

    public InteractionReportController(
        InteractionReportService interactionReportService,
        PageViewService pageViewService,
        MessagingReportService messagingReportService,
        UserService userService
    ) {
        this.interactionReportService = interactionReportService;
        this.pageViewService = pageViewService;
        this.messagingReportService = messagingReportService;
        this.userService = userService;
    }

    @GetMapping("/inactive-users")
    @PreAuthorize("hasRole('ADMIN')")
    public InactiveUsersReportResponse inactiveUsers(@RequestParam(defaultValue = "7") int days) {
        return interactionReportService.generateInactiveUsersReport(days);
    }

    @GetMapping("/usage-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public UsageSummaryResponse usageSummary(
        @RequestParam LocalDate from,
        @RequestParam LocalDate to
    ) {
        return pageViewService.getUsageSummary(from, to);
    }

    @GetMapping("/messaging-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public MessagingReportResponse messagingSummary(
        @RequestParam(name = "windowDays", defaultValue = "7") int windowDays
    ) {
        return messagingReportService.generateMessagingReport(windowDays);
    }

    @GetMapping("/students-without-tutor")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> studentsWithoutTutor(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.ASC, "username"));
        return userService.listStudentsWithoutActiveCurrentTutor(pageable);
    }
}
