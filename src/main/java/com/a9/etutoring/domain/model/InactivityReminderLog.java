package com.a9.etutoring.domain.model;

import com.a9.etutoring.domain.enums.InactivityReminderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inactivity_reminder_log")
public class InactivityReminderLog {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_user_id", nullable = false)
    private User tutor;

    @Column(name = "activity_baseline_at", nullable = false)
    private Instant activityBaselineAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InactivityReminderStatus status;

    @Column(name = "student_sent_at")
    private Instant studentSentAt;

    @Column(name = "tutor_sent_at")
    private Instant tutorSentAt;

    @Column(name = "student_error", columnDefinition = "TEXT")
    private String studentError;

    @Column(name = "tutor_error", columnDefinition = "TEXT")
    private String tutorError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public User getTutor() {
        return tutor;
    }

    public void setTutor(User tutor) {
        this.tutor = tutor;
    }

    public Instant getActivityBaselineAt() {
        return activityBaselineAt;
    }

    public void setActivityBaselineAt(Instant activityBaselineAt) {
        this.activityBaselineAt = activityBaselineAt;
    }

    public InactivityReminderStatus getStatus() {
        return status;
    }

    public void setStatus(InactivityReminderStatus status) {
        this.status = status;
    }

    public Instant getStudentSentAt() {
        return studentSentAt;
    }

    public void setStudentSentAt(Instant studentSentAt) {
        this.studentSentAt = studentSentAt;
    }

    public Instant getTutorSentAt() {
        return tutorSentAt;
    }

    public void setTutorSentAt(Instant tutorSentAt) {
        this.tutorSentAt = tutorSentAt;
    }

    public String getStudentError() {
        return studentError;
    }

    public void setStudentError(String studentError) {
        this.studentError = studentError;
    }

    public String getTutorError() {
        return tutorError;
    }

    public void setTutorError(String tutorError) {
        this.tutorError = tutorError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
