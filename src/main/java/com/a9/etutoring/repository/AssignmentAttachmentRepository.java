package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.AssignmentAttachment;
import com.a9.etutoring.repository.projection.AssignmentAttachmentSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentAttachmentRepository extends JpaRepository<AssignmentAttachment, UUID> {

    List<AssignmentAttachmentSummary> findByAssignment_Id(UUID assignmentId);

    List<AssignmentAttachment> findAllByAssignment_Id(UUID assignmentId);

    void deleteAllByAssignment_Id(UUID assignmentId);

    Optional<AssignmentAttachment> findByIdAndAssignment_Id(UUID id, UUID assignmentId);
}
