package com.a9.etutoring.domain.dto.assignment;

import com.a9.etutoring.domain.dto.blog.AttachmentResponse;
import com.a9.etutoring.domain.enums.AssignmentSubmissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignmentSubmissionSummaryResponse(
    UUID id,
    UUID studentId,
    AssignmentSubmissionStatus status,
    Instant submittedAt,
    Instant updatedAt,
    boolean hasFeedback,
    List<AttachmentResponse> submissionAttachments
) {}
