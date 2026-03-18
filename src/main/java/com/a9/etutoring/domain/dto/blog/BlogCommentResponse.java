package com.a9.etutoring.domain.dto.blog;

import java.time.Instant;
import java.util.UUID;

public record BlogCommentResponse(
    UUID id,
    UUID postId,
    UUID authorUserId,
    String comment,
    Instant createdDate,
    Instant updatedDate
) {
}
