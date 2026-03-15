package com.a9.etutoring.domain.dto.conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
    UUID id,
    UUID studentUserId,
    UUID tutorUserId,
    Instant createdDate
) {
}
