package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.dto.report.InactiveUserResponse;
import com.a9.etutoring.domain.dto.report.InactiveUsersReportResponse;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.service.InteractionReportService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InteractionReportServiceImpl implements InteractionReportService {

    private final UserRepository userRepository;

    public InteractionReportServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public InactiveUsersReportResponse generateInactiveUsersReport(int days) {
        if (days != 7 && days != 28) {
            throw new BadRequestException("INVALID_REPORT_RANGE", "Days must be exactly 7 or 28");
        }

        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofDays(days));

        List<InactiveUserResponse> students = userRepository.findInactiveUsersByRole(UserRole.STUDENT, cutoff)
            .stream()
            .map(user -> toInactiveUser(user, now))
            .toList();

        List<InactiveUserResponse> tutors = userRepository.findInactiveUsersByRole(UserRole.TUTOR, cutoff)
            .stream()
            .map(user -> toInactiveUser(user, now))
            .toList();

        return new InactiveUsersReportResponse(days, now, students, tutors);
    }

    private InactiveUserResponse toInactiveUser(User user, Instant now) {
        Instant baseline = user.getLastInteractionDate() != null ? user.getLastInteractionDate() : user.getCreatedDate();
        long inactivityDays = Duration.between(baseline, now).toDays();

        return new InactiveUserResponse(
            user.getId(),
            user.getRole(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getLastInteractionDate(),
            inactivityDays
        );
    }
}
