package com.a9.etutoring.service;

import com.a9.etutoring.domain.dto.blog.BlogCommentResponse;
import com.a9.etutoring.domain.dto.blog.BlogCreateCommentRequest;
import com.a9.etutoring.domain.dto.blog.BlogPostResponse;
import com.a9.etutoring.domain.enums.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface BlogService {

    BlogPostResponse createPost(UUID creatorId, String body, List<UUID> targetStudentIds, List<MultipartFile> attachments);

    BlogPostResponse updatePost(
        UUID postId,
        UUID actorId,
        String body,
        List<UUID> targetStudentIds,
        List<MultipartFile> attachments,
        List<UUID> keepAttachmentIds
    );

    BlogPostResponse targetStudents(UUID postId, List<UUID> studentIds);

    BlogCommentResponse addComment(UUID postId, UUID authorId, BlogCreateCommentRequest request);

    BlogCommentResponse updateComment(UUID commentId, UUID authorId, BlogCreateCommentRequest request);

    BlogPostResponse getPost(UUID postId, UUID requesterId);

    List<BlogPostResponse> listVisiblePosts(UUID userId, UserRole role);

    void deletePost(UUID postId, UUID actorId);

    ResponseEntity<byte[]> downloadAttachment(UUID attachmentId, UUID requesterId);
}
