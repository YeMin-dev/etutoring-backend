package com.a9.etutoring.service;

import com.a9.etutoring.domain.dto.allocation.AllocationCreateRequest;
import com.a9.etutoring.domain.dto.allocation.AllocationPreviewRequest;
import com.a9.etutoring.domain.dto.allocation.AllocationUpdateRequest;
import com.a9.etutoring.domain.dto.allocation.BulkAllocationPreviewResponse;
import com.a9.etutoring.domain.dto.allocation.BulkAllocationRequest;
import com.a9.etutoring.domain.dto.allocation.AllocatedStudentResponse;
import com.a9.etutoring.domain.dto.allocation.TutorAllocationResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TutorAllocationService {

    List<AllocatedStudentResponse> listAllocatedStudentsForTutor(UUID tutorId);

    TutorAllocationResponse allocate(AllocationCreateRequest request);

    List<TutorAllocationResponse> allocateBulk(BulkAllocationRequest request);

    BulkAllocationPreviewResponse previewBulkAllocation(AllocationPreviewRequest request);

    Page<TutorAllocationResponse> list(Pageable pageable, String search);

    TutorAllocationResponse undo(UUID id);

    TutorAllocationResponse update(UUID id, AllocationUpdateRequest request);
}
