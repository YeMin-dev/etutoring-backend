package com.a9.etutoring.domain.dto.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record BlogCreatePostRequest(
    @NotBlank @Size(max = 5000) String body,
    List<@NotBlank @Size(max = 255) String> attachments,
    List<UUID> targetStudentIds
) {
}
