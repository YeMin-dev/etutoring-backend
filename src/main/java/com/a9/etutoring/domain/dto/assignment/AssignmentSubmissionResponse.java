package com.a9.etutoring.domain.dto.assignment;

import com.a9.etutoring.domain.dto.blog.AttachmentResponse;
import com.a9.etutoring.domain.enums.AssignmentSubmissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignmentSubmissionResponse(
    UUID id,
    UUID assignmentId,
    UUID studentId,
    AssignmentSubmissionStatus status,
    Instant submittedAt,
    Instant updatedAt,
    String feedbackText,
    Instant feedbackAt,
    UUID feedbackById,
    List<AttachmentResponse> attachments
) {}
