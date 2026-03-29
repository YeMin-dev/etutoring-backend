package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.SubmissionAttachment;
import com.a9.etutoring.repository.projection.SubmissionAttachmentSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionAttachmentRepository extends JpaRepository<SubmissionAttachment, UUID> {

    List<SubmissionAttachmentSummary> findBySubmission_Id(UUID submissionId);

    void deleteAllBySubmission_Id(UUID submissionId);

    Optional<SubmissionAttachment> findByIdAndSubmission_Id(UUID id, UUID submissionId);
}
