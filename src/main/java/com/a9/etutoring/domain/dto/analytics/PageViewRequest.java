package com.a9.etutoring.domain.dto.analytics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PageViewRequest(
    @NotBlank
    @Size(max = 255)
    String pagePath
) {}
