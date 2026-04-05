package com.a9.etutoring.domain.dto.analytics;

import java.util.UUID;

public record UserActivityItem(UUID userId, String username, String email, long viewCount) {}
