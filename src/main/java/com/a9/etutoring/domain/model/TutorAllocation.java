package com.a9.etutoring.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tutor_allocation")
public class TutorAllocation {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_user_id", nullable = false)
    private User tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_by_id", nullable = false)
    private User allocatedBy;

    @Column(name = "allocated_date", nullable = false)
    private Instant allocatedDate;

    @Column(name = "ended_date")
    private Instant endedDate;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "schedule_start")
    private Instant scheduleStart;

    @Column(name = "schedule_end")
    private Instant scheduleEnd;

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

    public User getAllocatedBy() {
        return allocatedBy;
    }

    public void setAllocatedBy(User allocatedBy) {
        this.allocatedBy = allocatedBy;
    }

    public Instant getAllocatedDate() {
        return allocatedDate;
    }

    public void setAllocatedDate(Instant allocatedDate) {
        this.allocatedDate = allocatedDate;
    }

    public Instant getEndedDate() {
        return endedDate;
    }

    public void setEndedDate(Instant endedDate) {
        this.endedDate = endedDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getScheduleStart() {
        return scheduleStart;
    }

    public void setScheduleStart(Instant scheduleStart) {
        this.scheduleStart = scheduleStart;
    }

    public Instant getScheduleEnd() {
        return scheduleEnd;
    }

    public void setScheduleEnd(Instant scheduleEnd) {
        this.scheduleEnd = scheduleEnd;
    }
}
