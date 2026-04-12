package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.dto.report.MessagingReportResponse;
import com.a9.etutoring.domain.dto.report.TutorMessagingStatsRow;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.repository.MessageRepository;
import com.a9.etutoring.repository.TutorAllocationRepository;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.service.MessagingReportService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MessagingReportServiceImpl implements MessagingReportService {

    private static final int DEFAULT_WINDOW_DAYS = 7;
    private static final int MIN_WINDOW_DAYS = 1;
    private static final int MAX_WINDOW_DAYS = 90;

    private final MessageRepository messageRepository;
    private final TutorAllocationRepository tutorAllocationRepository;
    private final UserRepository userRepository;

    public MessagingReportServiceImpl(
        MessageRepository messageRepository,
        TutorAllocationRepository tutorAllocationRepository,
        UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.tutorAllocationRepository = tutorAllocationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public MessagingReportResponse generateMessagingReport(int windowDays) {
        int days = windowDays > 0 ? windowDays : DEFAULT_WINDOW_DAYS;
        if (days < MIN_WINDOW_DAYS || days > MAX_WINDOW_DAYS) {
            throw new BadRequestException(
                "INVALID_REPORT_RANGE",
                "windowDays must be between " + MIN_WINDOW_DAYS + " and " + MAX_WINDOW_DAYS + " inclusive");
        }

        Instant windowEndExclusive = Instant.now();
        Instant windowStart = windowEndExclusive.minus(days, ChronoUnit.DAYS);

        long total = messageRepository.countByCreatedDateBetween(windowStart, windowEndExclusive);

        List<UUID> tutorIds = tutorAllocationRepository.findDistinctActiveTutorUserIds();
        List<Object[]> countRows = messageRepository.countMessagesGroupedByTutorBetween(windowStart, windowEndExclusive);
        Map<UUID, Long> countByTutor = new HashMap<>();
        for (Object[] row : countRows) {
            UUID tutorId = (UUID) row[0];
            long cnt = row[1] instanceof Number n ? n.longValue() : 0L;
            countByTutor.put(tutorId, cnt);
        }

        if (tutorIds.isEmpty()) {
            return new MessagingReportResponse(windowStart, windowEndExclusive, days, total, List.of());
        }

        List<User> tutors = new ArrayList<>(userRepository.findAllByIdInAndDeletedDateIsNull(tutorIds));
        tutors.sort(Comparator.comparing(User::getUsername, Comparator.nullsLast(String::compareToIgnoreCase)));

        BigDecimal divisor = BigDecimal.valueOf(days);
        List<TutorMessagingStatsRow> rows = new ArrayList<>();
        for (User t : tutors) {
            long cnt = countByTutor.getOrDefault(t.getId(), 0L);
            BigDecimal avg = BigDecimal.valueOf(cnt)
                .divide(divisor, 2, RoundingMode.HALF_UP);
            rows.add(new TutorMessagingStatsRow(
                t.getId(),
                t.getUsername(),
                t.getEmail(),
                t.getFirstName(),
                t.getLastName(),
                cnt,
                avg));
        }

        return new MessagingReportResponse(windowStart, windowEndExclusive, days, total, rows);
    }
}
