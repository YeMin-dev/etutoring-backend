package com.a9.etutoring.domain.dto.assignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignmentFeedbackRequest(
    @NotBlank @Size(max = 5000) String feedbackText
) {}
