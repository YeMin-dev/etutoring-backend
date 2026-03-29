package com.a9.etutoring.domain.model;

import com.a9.etutoring.domain.enums.AssignmentSubmissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "assignment_submissions")
public class AssignmentSubmission {

    @Id
    @EqualsAndHashCode.Include
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AssignmentSubmissionStatus status;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "feedback_at")
    private Instant feedbackAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_by_id")
    private User feedbackBy;

    @Builder.Default
    @OneToMany(mappedBy = "submission", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<SubmissionAttachment> attachments = new ArrayList<>();
}
