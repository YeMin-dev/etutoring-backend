package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.blog.BlogCommentResponse;
import com.a9.etutoring.domain.dto.blog.BlogCreateCommentRequest;
import com.a9.etutoring.domain.dto.blog.BlogCreatePostRequest;
import com.a9.etutoring.domain.dto.blog.BlogPostResponse;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.BlogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TUTOR','ADMIN')")
    public BlogPostResponse createPost(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody BlogCreatePostRequest request
    ) {
        return blogService.createPost(principal.getId(), request);
    }

    @PostMapping("/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TUTOR','ADMIN')")
    public BlogPostResponse uploadAttachment(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam("file") MultipartFile file
    ) {
        return blogService.uploadAttachment(id, principal.getId(), file);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STUDENT','TUTOR','ADMIN')")
    public BlogCommentResponse addComment(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody BlogCreateCommentRequest request
    ) {
        return blogService.addComment(id, principal.getId(), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','TUTOR','ADMIN')")
    public List<BlogPostResponse> listVisiblePosts(@AuthenticationPrincipal UserPrincipal principal) {
        return blogService.listVisiblePosts(principal.getId(), principal.getRole());
    }
}
