package com.a9.etutoring.domain.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageCreateRequest(
    @NotBlank(message = "body is required")
    @Size(max = 10000)
    String body
) {
}
