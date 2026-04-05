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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InactivityReminderServiceImplTest {

    @Mock
    private TutorAllocationRepository allocationRepository;
    @Mock
    private InactivityReminderLogRepository logRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private TransactionTemplate transactionTemplate;

    private InactivityReminderServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> c = invocation.getArgument(0);
            c.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        service = new InactivityReminderServiceImpl(
            allocationRepository,
            logRepository,
            userRepository,
            emailService,
            transactionTemplate,
            28);
    }

    @Test
    void runDueReminders_emptyAllocations_doesNothing() {
        when(allocationRepository.findEligibleForInactivityReminder(any(), any())).thenReturn(List.of());

        service.runDueReminders();

        verify(logRepository, never()).saveAndFlush(any());
        verify(emailService, never()).sendEmailSync(any(), any(), any());
    }

    @Test
    void runDueReminders_eligible_sendsBothEmailsAndMarksSent() {
        Instant oldInteraction = Instant.now().minus(40, ChronoUnit.DAYS);
        User student = studentUser(oldInteraction);
        User tutor = tutorUser();
        TutorAllocation ta = allocation(student, tutor);

        when(allocationRepository.findEligibleForInactivityReminder(any(), any())).thenReturn(List.of(ta));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(userRepository.findById(tutor.getId())).thenReturn(Optional.of(tutor));
        when(logRepository.existsByStudent_IdAndTutor_IdAndActivityBaselineAt(
            student.getId(), tutor.getId(), oldInteraction)).thenReturn(false);
        when(emailService.sendEmailSync(any(), any(), any())).thenReturn(true);

        service.runDueReminders();

        verify(logRepository).saveAndFlush(any());
        verify(emailService).sendEmailSync(eq(student.getEmail()), eq("Inactivity reminder – eTutoring"), any());
        verify(emailService).sendEmailSync(eq(tutor.getEmail()), eq("Inactivity reminder – eTutoring"), any());

        ArgumentCaptor<InactivityReminderLog> captor = ArgumentCaptor.forClass(InactivityReminderLog.class);
        verify(logRepository).save(captor.capture());
        assertEquals(InactivityReminderStatus.SENT, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getStudentSentAt());
        assertNotNull(captor.getValue().getTutorSentAt());
    }

    @Test
    void runDueReminders_whenAlreadyLogged_skipsSend() {
        Instant oldInteraction = Instant.now().minus(40, ChronoUnit.DAYS);
        User student = studentUser(oldInteraction);
        User tutor = tutorUser();
        TutorAllocation ta = allocation(student, tutor);

        when(allocationRepository.findEligibleForInactivityReminder(any(), any())).thenReturn(List.of(ta));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(userRepository.findById(tutor.getId())).thenReturn(Optional.of(tutor));
        when(logRepository.existsByStudent_IdAndTutor_IdAndActivityBaselineAt(
            student.getId(), tutor.getId(), oldInteraction)).thenReturn(true);

        service.runDueReminders();

        verify(logRepository, never()).saveAndFlush(any());
        verify(emailService, never()).sendEmailSync(any(), any(), any());
    }

    @Test
    void runDueReminders_uniqueViolationOnInsert_skipsEmail() {
        Instant oldInteraction = Instant.now().minus(40, ChronoUnit.DAYS);
        User student = studentUser(oldInteraction);
        User tutor = tutorUser();
        TutorAllocation ta = allocation(student, tutor);

        when(allocationRepository.findEligibleForInactivityReminder(any(), any())).thenReturn(List.of(ta));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(userRepository.findById(tutor.getId())).thenReturn(Optional.of(tutor));
        when(logRepository.existsByStudent_IdAndTutor_IdAndActivityBaselineAt(
            student.getId(), tutor.getId(), oldInteraction)).thenReturn(false);
        when(logRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));

        service.runDueReminders();

        verify(emailService, never()).sendEmailSync(any(), any(), any());
    }

    @Test
    void runDueReminders_oneEmailFails_statusPartial() {
        Instant oldInteraction = Instant.now().minus(40, ChronoUnit.DAYS);
        User student = studentUser(oldInteraction);
        User tutor = tutorUser();
        TutorAllocation ta = allocation(student, tutor);

        when(allocationRepository.findEligibleForInactivityReminder(any(), any())).thenReturn(List.of(ta));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(userRepository.findById(tutor.getId())).thenReturn(Optional.of(tutor));
        when(logRepository.existsByStudent_IdAndTutor_IdAndActivityBaselineAt(
            student.getId(), tutor.getId(), oldInteraction)).thenReturn(false);
        when(emailService.sendEmailSync(eq(student.getEmail()), eq("Inactivity reminder – eTutoring"), any()))
            .thenReturn(true);
        when(emailService.sendEmailSync(eq(tutor.getEmail()), eq("Inactivity reminder – eTutoring"), any()))
            .thenReturn(false);

        service.runDueReminders();

        ArgumentCaptor<InactivityReminderLog> captor = ArgumentCaptor.forClass(InactivityReminderLog.class);
        verify(logRepository).save(captor.capture());
        assertEquals(InactivityReminderStatus.PARTIAL, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getStudentSentAt());
        assertEquals(null, captor.getValue().getTutorSentAt());
        assertNotNull(captor.getValue().getTutorError());
    }

    private static User studentUser(Instant lastInteraction) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setRole(UserRole.STUDENT);
        u.setUsername("stu1");
        u.setFirstName("Sam");
        u.setLastName("Student");
        u.setEmail("sam@example.com");
        u.setPassword("x");
        u.setIsActive(true);
        u.setIsLocked(false);
        u.setCreatedDate(Instant.now().minus(400, ChronoUnit.DAYS));
        u.setLastInteractionDate(lastInteraction);
        u.setDeletedDate(null);
        return u;
    }

    private static User tutorUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setRole(UserRole.TUTOR);
        u.setUsername("tut1");
        u.setFirstName("Terry");
        u.setLastName("Tutor");
        u.setEmail("terry@example.com");
        u.setPassword("x");
        u.setIsActive(true);
        u.setIsLocked(false);
        u.setCreatedDate(Instant.now().minus(200, ChronoUnit.DAYS));
        u.setDeletedDate(null);
        return u;
    }

    private static TutorAllocation allocation(User student, User tutor) {
        TutorAllocation ta = new TutorAllocation();
        ta.setId(UUID.randomUUID());
        ta.setStudent(student);
        ta.setTutor(tutor);
        return ta;
    }
}
