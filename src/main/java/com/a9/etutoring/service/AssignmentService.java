package com.a9.etutoring.service;

import com.a9.etutoring.domain.dto.assignment.AssignmentFeedbackRequest;
import com.a9.etutoring.domain.dto.assignment.AssignmentResponse;
import com.a9.etutoring.domain.dto.assignment.AssignmentSubmissionResponse;
import com.a9.etutoring.domain.dto.assignment.AssignmentSummaryResponse;
import com.a9.etutoring.domain.enums.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface AssignmentService {

    AssignmentResponse createAssignment(
        UUID tutorId,
        String title,
        String instructions,
        String dueDate,
        List<MultipartFile> attachments
    );

    AssignmentResponse updateAssignment(
        UUID assignmentId,
        UUID tutorId,
        String title,
        String instructions,
        String dueDate,
        List<UUID> keepAttachmentIds,
        List<MultipartFile> attachments
    );

    void deleteAssignment(UUID assignmentId, UUID tutorId);

    List<AssignmentSummaryResponse> listForTutor(UUID tutorId);

    AssignmentResponse getForTutor(UUID assignmentId, UUID tutorId);

    AssignmentSubmissionResponse getSubmissionForTutor(UUID assignmentId, UUID submissionId, UUID tutorId);

    void setFeedback(UUID assignmentId, UUID submissionId, UUID tutorId, AssignmentFeedbackRequest request);

    List<AssignmentSummaryResponse> listForStudent(UUID studentId);

    AssignmentResponse getForStudent(UUID assignmentId, UUID studentId);

    AssignmentSubmissionResponse getOrCreateSubmissionForStudent(UUID assignmentId, UUID studentId);

    AssignmentSubmissionResponse submitAssignment(UUID assignmentId, UUID studentId, List<MultipartFile> files);

    ResponseEntity<byte[]> downloadAssignmentAttachment(UUID attachmentId, UUID userId, UserRole role);

    ResponseEntity<byte[]> downloadSubmissionAttachment(UUID attachmentId, UUID userId, UserRole role);
}
