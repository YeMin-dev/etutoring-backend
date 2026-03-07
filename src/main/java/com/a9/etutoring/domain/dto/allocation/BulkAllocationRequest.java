package com.a9.etutoring.domain.dto.allocation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkAllocationRequest(
    @NotEmpty @Size(max = 500) @Valid List<AllocationCreateRequest> items
) {
}
