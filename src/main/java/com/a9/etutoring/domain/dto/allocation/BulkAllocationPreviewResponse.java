package com.a9.etutoring.domain.dto.allocation;

import java.util.List;

public record BulkAllocationPreviewResponse(
    List<AllocationPreviewItemResponse> items
) {
}
