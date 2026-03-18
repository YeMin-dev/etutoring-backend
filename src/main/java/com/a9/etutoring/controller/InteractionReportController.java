package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.report.InactiveUsersReportResponse;
import com.a9.etutoring.service.InteractionReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
public class InteractionReportController {

    private final InteractionReportService interactionReportService;

    public InteractionReportController(InteractionReportService interactionReportService) {
        this.interactionReportService = interactionReportService;
    }

    @GetMapping("/inactive-users")
    @PreAuthorize("hasRole('ADMIN')")
    public InactiveUsersReportResponse inactiveUsers(@RequestParam(defaultValue = "7") int days) {
        return interactionReportService.generateInactiveUsersReport(days);
    }
}
