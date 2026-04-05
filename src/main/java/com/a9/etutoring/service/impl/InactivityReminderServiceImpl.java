package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.enums.InactivityReminderStatus;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.InactivityReminderLog;
import com.a9.etutoring.domain.model.TutorAllocation;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.repository.InactivityReminderLogRepository;
import com.a9.etutoring.repository.TutorAllocationRepository;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.service.EmailService;
import com.a9.etutoring.service.InactivityReminderService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class InactivityReminderServiceImpl implements InactivityReminderService {

    private static final int MAX_ERR_LEN = 1000;

    private final TutorAllocationRepository allocationRepository;
    private final InactivityReminderLogRepository logRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final TransactionTemplate txNew;
    private final int thresholdDays;

    public InactivityReminderServiceImpl(
        TutorAllocationRepository allocationRepository,
        InactivityReminderLogRepository logRepository,
        UserRepository userRepository,
        EmailService emailService,
        @Qualifier("inactivityReminderTx") TransactionTemplate inactivityReminderTransactionTemplate,
        @Value("${app.inactivity-reminder.threshold-days:28}") int thresholdDays) {
        this.allocationRepository = allocationRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.thresholdDays = thresholdDays;
        this.txNew = inactivityReminderTransactionTemplate;
    }

    @Override
    public void runDueReminders() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(thresholdDays));
        List<TutorAllocation> rows = allocationRepository.findEligibleForInactivityReminder(cutoff, UserRole.STUDENT);
        for (TutorAllocation ta : rows) {
            final TutorAllocation allocation = ta;
            txNew.executeWithoutResult(status -> dispatchPair(allocation, cutoff));
        }
    }

    private void dispatchPair(TutorAllocation allocation, Instant cutoff) {
        User student = userRepository.findById(allocation.getStudent().getId()).orElse(null);
        User tutor = userRepository.findById(allocation.getTutor().getId()).orElse(null);
        if (student == null || student.getDeletedDate() != null || student.getRole() != UserRole.STUDENT) {
            return;
        }
        if (tutor == null || tutor.getDeletedDate() != null) {
            return;
        }

        Instant baseline = student.getLastInteractionDate() != null
            ? student.getLastInteractionDate()
            : student.getCreatedDate();
        if (!baseline.isBefore(cutoff)) {
            return;
        }

        if (logRepository.existsByStudent_IdAndTutor_IdAndActivityBaselineAt(student.getId(), tutor.getId(), baseline)) {
            return;
        }

        InactivityReminderLog log = new InactivityReminderLog();
        log.setId(UUID.randomUUID());
        log.setStudent(student);
        log.setTutor(tutor);
        log.setActivityBaselineAt(baseline);
        log.setStatus(InactivityReminderStatus.PENDING);
        Instant created = Instant.now();
        log.setCreatedAt(created);
        log.setUpdatedAt(null);
        try {
            logRepository.saveAndFlush(log);
        } catch (DataIntegrityViolationException ex) {
            return;
        }

        String subject = "Inactivity reminder – eTutoring";
        long inactiveDays = Duration.between(baseline, Instant.now()).toDays();
        boolean studentOk = emailService.sendEmailSync(
            student.getEmail(),
            subject,
            buildStudentBody(student, tutor, inactiveDays));
        boolean tutorOk = emailService.sendEmailSync(
            tutor.getEmail(),
            subject,
            buildTutorBody(student, tutor, inactiveDays));

        Instant sendMark = Instant.now();
        if (studentOk) {
            log.setStudentSentAt(sendMark);
        } else {
            log.setStudentError(truncateError("Email send failed or mail not configured"));
        }
        if (tutorOk) {
            log.setTutorSentAt(sendMark);
        } else {
            log.setTutorError(truncateError("Email send failed or mail not configured"));
        }

        if (studentOk && tutorOk) {
            log.setStatus(InactivityReminderStatus.SENT);
        } else if (!studentOk && !tutorOk) {
            log.setStatus(InactivityReminderStatus.FAILED);
        } else {
            log.setStatus(InactivityReminderStatus.PARTIAL);
        }
        log.setUpdatedAt(Instant.now());
        logRepository.save(log);
    }

    private static String buildStudentBody(User student, User tutor, long inactiveDays) {
        return String.format(
            "Hello %s,\n\n"
                + "We have not recorded any activity on your eTutoring account for approximately %d days "
                + "(based on your last login or other tracked activity). "
                + "Please sign in when you can to stay in touch with your studies and your personal tutor (%s).\n\n"
                + "If you believe this message is wrong, you can ignore it or contact support.\n\n"
                + "Best regards,\neTutoring System",
            buildFullName(student),
            inactiveDays,
            buildFullName(tutor));
    }

    private static String buildTutorBody(User student, User tutor, long inactiveDays) {
        return String.format(
            "Hello %s,\n\n"
                + "Your allocated student %s has had no recorded eTutoring activity for approximately %d days. "
                + "You may wish to reach out to encourage re-engagement.\n\n"
                + "Best regards,\neTutoring System",
            buildFullName(tutor),
            buildFullName(student),
            inactiveDays);
    }

    private static String buildFullName(User user) {
        if (user.getFirstName() != null && !user.getFirstName().isBlank()
            && user.getLastName() != null && !user.getLastName().isBlank()) {
            return user.getFirstName() + " " + user.getLastName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }

    private static String truncateError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERR_LEN ? message : message.substring(0, MAX_ERR_LEN);
    }
}
