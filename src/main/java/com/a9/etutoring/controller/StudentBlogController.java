package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.blog.BlogCommentResponse;
import com.a9.etutoring.domain.dto.blog.BlogCreateCommentRequest;
import com.a9.etutoring.domain.dto.blog.BlogPostResponse;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.BlogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/blogs")
public class StudentBlogController {

    private final BlogService blogService;

    public StudentBlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping
    public List<BlogPostResponse> listBlogs(@AuthenticationPrincipal UserPrincipal principal) {
        return blogService.listVisiblePosts(principal.getId(), UserRole.STUDENT);
    }

    @GetMapping("/{id}")
    public BlogPostResponse getBlog(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return blogService.getPost(id, principal.getId());
    }

    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogCommentResponse addComment(
        @PathVariable UUID postId,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody BlogCreateCommentRequest request
    ) {
        return blogService.addComment(postId, principal.getId(), request);
    }

    @PutMapping("/comments/{commentId}")
    public BlogCommentResponse updateComment(
        @PathVariable UUID commentId,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody BlogCreateCommentRequest request
    ) {
        return blogService.updateComment(commentId, principal.getId(), request);
    }
}
