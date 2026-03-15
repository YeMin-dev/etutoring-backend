package com.a9.etutoring.domain.dto.conversation;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateConversationRequest(
    @NotNull(message = "tutorUserId is required") UUID tutorUserId,
    @NotNull(message = "studentUserId is required") UUID studentUserId
) {
}
