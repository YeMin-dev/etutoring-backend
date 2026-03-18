package com.a9.etutoring.controller;

import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.BlogService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blogs")
public class BlogAttachmentController {

    private final BlogService blogService;

    public BlogAttachmentController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(
        @PathVariable UUID attachmentId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return blogService.downloadAttachment(attachmentId, principal.getId());
    }
}
