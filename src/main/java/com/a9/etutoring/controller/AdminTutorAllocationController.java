package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.allocation.AllocationCreateRequest;
import com.a9.etutoring.domain.dto.allocation.AllocationPreviewRequest;
import com.a9.etutoring.domain.dto.allocation.AllocationUpdateRequest;
import com.a9.etutoring.domain.dto.allocation.BulkAllocationPreviewResponse;
import com.a9.etutoring.domain.dto.allocation.BulkAllocationRequest;
import com.a9.etutoring.domain.dto.allocation.TutorAllocationResponse;
import com.a9.etutoring.service.TutorAllocationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminTutorAllocationController {

    private final TutorAllocationService tutorAllocationService;

    public AdminTutorAllocationController(TutorAllocationService tutorAllocationService) {
        this.tutorAllocationService = tutorAllocationService;
    }

    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping("/allocations")
    public Page<TutorAllocationResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String search
    ) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "allocatedDate"));
        return tutorAllocationService.list(pageable, search);
    }

    @PostMapping("/allocations")
    @ResponseStatus(HttpStatus.CREATED)
    public TutorAllocationResponse create(@Valid @RequestBody AllocationCreateRequest request) {
        return tutorAllocationService.allocate(request);
    }

    @PostMapping("/allocations/bulk")
    public List<TutorAllocationResponse> createBulk(@Valid @RequestBody BulkAllocationRequest request) {
        return tutorAllocationService.allocateBulk(request);
    }

    @PostMapping("/allocations/preview")
    public BulkAllocationPreviewResponse previewBulk(@Valid @RequestBody AllocationPreviewRequest request) {
        return tutorAllocationService.previewBulkAllocation(request);
    }

    @PostMapping("/allocations/{id}/undo")
    public TutorAllocationResponse undo(@PathVariable UUID id) {
        return tutorAllocationService.undo(id);
    }

    @PutMapping("/allocations/{id}")
    public TutorAllocationResponse update(@PathVariable UUID id, @Valid @RequestBody AllocationUpdateRequest request) {
        return tutorAllocationService.update(id, request);
    }
}
