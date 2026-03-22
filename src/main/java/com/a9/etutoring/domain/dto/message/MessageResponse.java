package com.a9.etutoring.domain.dto.message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    UUID conversationId,
    UUID senderUserId,
    String body,
    Instant createdDate,
    Instant readDate
) {
}
