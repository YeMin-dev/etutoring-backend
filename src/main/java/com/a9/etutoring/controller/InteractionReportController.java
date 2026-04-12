package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.analytics.UsageSummaryResponse;
import com.a9.etutoring.domain.dto.report.InactiveUsersReportResponse;
import com.a9.etutoring.domain.dto.report.MessagingReportResponse;
import com.a9.etutoring.service.InteractionReportService;
import com.a9.etutoring.service.MessagingReportService;
import com.a9.etutoring.service.PageViewService;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
public class InteractionReportController {

    private final InteractionReportService interactionReportService;
    private final PageViewService pageViewService;
    private final MessagingReportService messagingReportService;

    public InteractionReportController(
        InteractionReportService interactionReportService,
        PageViewService pageViewService,
        MessagingReportService messagingReportService
    ) {
        this.interactionReportService = interactionReportService;
        this.pageViewService = pageViewService;
        this.messagingReportService = messagingReportService;
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
}
