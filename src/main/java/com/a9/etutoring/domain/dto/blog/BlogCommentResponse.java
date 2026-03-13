package com.a9.etutoring.domain.dto.blog;

import java.time.Instant;
import java.util.UUID;

public record BlogCommentResponse(
    UUID id,
    UUID postId,
    UUID authorUserId,
    UUID attachmentId,
    String comment,
    Instant createdDate,
    Instant updatedDate
) {
}
