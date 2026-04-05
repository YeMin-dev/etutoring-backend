package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.analytics.UsageSummaryResponse;
import com.a9.etutoring.domain.dto.report.InactiveUsersReportResponse;
import com.a9.etutoring.service.InteractionReportService;
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

    public InteractionReportController(
        InteractionReportService interactionReportService,
        PageViewService pageViewService
    ) {
        this.interactionReportService = interactionReportService;
        this.pageViewService = pageViewService;
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
}
