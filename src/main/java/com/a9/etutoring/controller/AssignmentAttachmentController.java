package com.a9.etutoring.controller;

import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.AssignmentService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentAttachmentController {

    private final AssignmentService assignmentService;

    public AssignmentAttachmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAssignmentAttachment(
        @PathVariable UUID attachmentId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return assignmentService.downloadAssignmentAttachment(attachmentId, principal.getId(), principal.getRole());
    }

    @GetMapping("/submissions/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadSubmissionAttachment(
        @PathVariable UUID attachmentId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return assignmentService.downloadSubmissionAttachment(attachmentId, principal.getId(), principal.getRole());
    }
}
