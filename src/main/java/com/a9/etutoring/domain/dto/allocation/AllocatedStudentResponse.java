package com.a9.etutoring.domain.dto.allocation;

import com.a9.etutoring.domain.dto.user.UserResponse;
import java.util.List;

public record AllocatedStudentResponse(
    UserResponse student,
    List<AllocationSlotResponse> allocationSlots
) {
}
