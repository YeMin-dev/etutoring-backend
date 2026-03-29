package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.AssignmentSubmission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    Optional<AssignmentSubmission> findByAssignment_IdAndStudent_Id(UUID assignmentId, UUID studentId);

    List<AssignmentSubmission> findAllByAssignment_IdOrderBySubmittedAtAsc(UUID assignmentId);

    Optional<AssignmentSubmission> findByIdAndAssignment_Id(UUID id, UUID assignmentId);
}
