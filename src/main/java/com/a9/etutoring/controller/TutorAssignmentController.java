package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.assignment.AssignmentFeedbackRequest;
import com.a9.etutoring.domain.dto.assignment.AssignmentResponse;
import com.a9.etutoring.domain.dto.assignment.AssignmentSubmissionResponse;
import com.a9.etutoring.domain.dto.assignment.AssignmentSummaryResponse;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.AssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tutor/assignments")
public class TutorAssignmentController {

    private final AssignmentService assignmentService;

    public TutorAssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentResponse create(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestPart("title") String title,
        @RequestPart("instructions") String instructions,
        @RequestPart(value = "dueDate", required = false) String dueDate,
        @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
    ) {
        return assignmentService.createAssignment(principal.getId(), title, instructions, dueDate, attachments);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssignmentResponse update(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestPart("title") String title,
        @RequestPart("instructions") String instructions,
        @RequestPart(value = "dueDate", required = false) String dueDate,
        @RequestParam(value = "keepAttachmentIds", required = false) List<UUID> keepAttachmentIds,
        @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
    ) {
        return assignmentService.updateAssignment(
            id, principal.getId(), title, instructions, dueDate, keepAttachmentIds, attachments
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        assignmentService.deleteAssignment(id, principal.getId());
    }

    @GetMapping
    public List<AssignmentSummaryResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return assignmentService.listForTutor(principal.getId());
    }

    @GetMapping("/{id}")
    public AssignmentResponse get(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return assignmentService.getForTutor(id, principal.getId());
    }

    @GetMapping("/{assignmentId}/submissions/{submissionId}")
    public AssignmentSubmissionResponse getSubmission(
        @PathVariable UUID assignmentId,
        @PathVariable UUID submissionId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return assignmentService.getSubmissionForTutor(assignmentId, submissionId, principal.getId());
    }

    @PutMapping("/{assignmentId}/submissions/{submissionId}/feedback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setFeedback(
        @PathVariable UUID assignmentId,
        @PathVariable UUID submissionId,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody AssignmentFeedbackRequest request
    ) {
        assignmentService.setFeedback(assignmentId, submissionId, principal.getId(), request);
    }
}
