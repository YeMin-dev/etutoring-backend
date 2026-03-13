package com.a9.etutoring.service;

import com.a9.etutoring.domain.dto.blog.BlogCommentResponse;
import com.a9.etutoring.domain.dto.blog.BlogCreateCommentRequest;
import com.a9.etutoring.domain.dto.blog.BlogCreatePostRequest;
import com.a9.etutoring.domain.dto.blog.BlogPostResponse;
import com.a9.etutoring.domain.enums.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface BlogService {

    BlogPostResponse createPost(UUID creatorId, BlogCreatePostRequest request);

    BlogPostResponse addAttachment(UUID postId, UUID actorId, String fileName);

    BlogPostResponse uploadAttachment(UUID postId, UUID actorId, MultipartFile file);

    BlogPostResponse targetStudents(UUID postId, List<UUID> studentIds);

    BlogCommentResponse addComment(UUID postId, UUID authorId, BlogCreateCommentRequest request);

    List<BlogPostResponse> listVisiblePosts(UUID userId, UserRole role);
}
