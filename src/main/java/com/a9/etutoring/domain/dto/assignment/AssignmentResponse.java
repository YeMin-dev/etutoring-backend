package com.a9.etutoring.domain.dto.assignment;

import com.a9.etutoring.domain.dto.blog.AttachmentResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssignmentResponse(
    UUID id,
    UUID createdById,
    String title,
    String instructions,
    Instant dueDate,
    Instant createdDate,
    Instant updatedDate,
    List<AttachmentResponse> attachments,
    List<AssignmentSubmissionSummaryResponse> submissions
) {}
