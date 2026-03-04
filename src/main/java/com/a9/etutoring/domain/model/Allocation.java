package com.a9.etutoring.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "allocations")
public class Allocation {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne
    @JoinColumn(name = "tutor_id", nullable = false)
    private User tutor;

    @Column(name = "allocated_date", nullable = false)
    private Instant allocatedDate;

    @Column(name = "deleted_date")
    private Instant deletedDate;

    // Constructors, getters, setters

    public Allocation() {}

    public Allocation(UUID id, User student, User tutor, Instant allocatedDate) {
        this.id = id;
        this.student = student;
        this.tutor = tutor;
        this.allocatedDate = allocatedDate;
    }

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

    public Instant getAllocatedDate() {
        return allocatedDate;
    }

    public void setAllocatedDate(Instant allocatedDate) {
        this.allocatedDate = allocatedDate;
    }

    public Instant getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(Instant deletedDate) {
        this.deletedDate = deletedDate;
    }
}