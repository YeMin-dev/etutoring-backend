package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.blog.BlogCommentResponse;
import com.a9.etutoring.domain.dto.blog.BlogCreateCommentRequest;
import com.a9.etutoring.domain.dto.blog.BlogPostResponse;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.BlogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tutor")
public class TutorBlogController {

    private final BlogService blogService;

    public TutorBlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @PostMapping("/blogs")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogPostResponse createPost(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestPart("body") String body,
        @RequestPart(value = "targetStudentIds", required = false) List<UUID> targetStudentIds,
        @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
    ) {
        return blogService.createPost(principal.getId(), body, targetStudentIds, attachments);
    }

    @PutMapping("/blogs/{id}")
    public BlogPostResponse updatePost(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestPart("body") String body,
        @RequestPart(value = "targetStudentIds", required = false) List<UUID> targetStudentIds,
        @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
        @RequestParam(value = "keepAttachmentIds", required = false) List<UUID> keepAttachmentIds
    ) {
        return blogService.updatePost(id, principal.getId(), body, targetStudentIds, attachments, keepAttachmentIds);
    }

    @DeleteMapping("/blogs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TUTOR')")
    public void deletePost(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        blogService.deletePost(id, principal.getId());
    }

    @PostMapping("/blogs/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogCommentResponse addComment(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody BlogCreateCommentRequest request
    ) {
        return blogService.addComment(id, principal.getId(), request);
    }

    @PutMapping("/blogs/comments/{commentId}")
    public BlogCommentResponse updateComment(
        @PathVariable UUID commentId,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody BlogCreateCommentRequest request
    ) {
        return blogService.updateComment(commentId, principal.getId(), request);
    }

    @GetMapping("/blogs/{id}")
    public BlogPostResponse getPost(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return blogService.getPost(id, principal.getId());
    }

    @GetMapping("/blogs")
    public List<BlogPostResponse> listVisiblePosts(@AuthenticationPrincipal UserPrincipal principal) {
        return blogService.listVisiblePosts(principal.getId(), principal.getRole());
    }
}
