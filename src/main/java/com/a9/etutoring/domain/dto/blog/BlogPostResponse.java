package com.a9.etutoring.domain.dto.blog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BlogPostResponse(
    UUID id,
    UUID createdById,
    String body,
    Instant createdDate,
    Instant updatedDate,
    List<String> attachments,
    List<UUID> targetStudentIds,
    List<BlogCommentResponse> comments
) {
}
