package com.a9.etutoring.domain.dto.conversation;

public record CreateConversationResult(
    boolean created,
    ConversationResponse conversation
) {
}
