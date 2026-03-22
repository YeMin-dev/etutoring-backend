package com.a9.etutoring.service;

import com.a9.etutoring.domain.dto.report.InactiveUsersReportResponse;

public interface InteractionReportService {

    InactiveUsersReportResponse generateInactiveUsersReport(int days);
}
