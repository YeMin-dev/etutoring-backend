package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.assignment.AssignmentResponse;
import com.a9.etutoring.domain.dto.assignment.AssignmentSubmissionResponse;
import com.a9.etutoring.domain.dto.assignment.AssignmentSummaryResponse;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.AssignmentService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/student/assignments")
public class StudentAssignmentController {

    private final AssignmentService assignmentService;

    public StudentAssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<AssignmentSummaryResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return assignmentService.listForStudent(principal.getId());
    }

    @GetMapping("/{id}")
    public AssignmentResponse get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return assignmentService.getForStudent(id, principal.getId());
    }

    @GetMapping("/{id}/submission")
    public AssignmentSubmissionResponse getSubmission(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return assignmentService.getOrCreateSubmissionForStudent(id, principal.getId());
    }

    @PutMapping(value = "/{id}/submission", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public AssignmentSubmissionResponse submit(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestPart("files") List<MultipartFile> files
    ) {
        return assignmentService.submitAssignment(id, principal.getId(), files);
    }
}
