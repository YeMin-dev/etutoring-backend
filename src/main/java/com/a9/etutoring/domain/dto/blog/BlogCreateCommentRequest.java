package com.a9.etutoring.domain.dto.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record BlogCreateCommentRequest(
    @NotBlank @Size(max = 5000) String comment,
    UUID attachmentId
) {
}
