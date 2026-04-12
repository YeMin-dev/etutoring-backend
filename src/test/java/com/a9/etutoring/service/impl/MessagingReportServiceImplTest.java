package com.a9.etutoring.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.a9.etutoring.domain.dto.report.MessagingReportResponse;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.repository.MessageRepository;
import com.a9.etutoring.repository.TutorAllocationRepository;
import com.a9.etutoring.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessagingReportServiceImplTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private TutorAllocationRepository tutorAllocationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessagingReportServiceImpl service;

    @Test
    void generateMessagingReport_invalidWindow_throws() {
        assertThrows(BadRequestException.class, () -> service.generateMessagingReport(91));
    }

    @Test
    void generateMessagingReport_buildsRows() {
        UUID tid = UUID.randomUUID();
        when(tutorAllocationRepository.findDistinctActiveTutorUserIds()).thenReturn(List.of(tid));
        when(messageRepository.countByCreatedDateBetween(any(), any())).thenReturn(10L);
        when(messageRepository.countMessagesGroupedByTutorBetween(any(), any()))
            .thenReturn(List.<Object[]>of(new Object[] {tid, 7L}));

        User tutor = new User();
        tutor.setId(tid);
        tutor.setUsername("t1");
        tutor.setEmail("t@x.com");
        tutor.setFirstName("T");
        tutor.setLastName("One");
        tutor.setRole(UserRole.TUTOR);
        when(userRepository.findAllByIdInAndDeletedDateIsNull(List.of(tid))).thenReturn(List.of(tutor));

        MessagingReportResponse r = service.generateMessagingReport(7);

        assertEquals(10L, r.totalMessagesInWindow());
        assertEquals(7, r.windowDays());
        assertEquals(1, r.tutors().size());
        assertEquals(7L, r.tutors().getFirst().messageCount());
        assertEquals(new BigDecimal("1.00"), r.tutors().getFirst().averageMessagesPerDay());
    }
}
