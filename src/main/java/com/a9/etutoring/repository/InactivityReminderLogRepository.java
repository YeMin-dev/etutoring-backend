package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.InactivityReminderLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InactivityReminderLogRepository extends JpaRepository<InactivityReminderLog, UUID> {

    boolean existsByStudent_IdAndTutor_IdAndActivityBaselineAt(UUID studentId, UUID tutorId, Instant activityBaselineAt);
}
