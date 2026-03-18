package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.dto.blog.AttachmentResponse;
import com.a9.etutoring.domain.dto.blog.BlogCommentResponse;
import com.a9.etutoring.domain.dto.blog.BlogCreateCommentRequest;
import com.a9.etutoring.domain.dto.blog.BlogPostResponse;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.Comment;
import com.a9.etutoring.domain.model.Post;
import com.a9.etutoring.domain.model.PostAttachment;
import com.a9.etutoring.domain.model.PostTarget;
import com.a9.etutoring.domain.model.PostTargetId;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.exception.ForbiddenException;
import com.a9.etutoring.exception.ResourceNotFoundException;
import com.a9.etutoring.repository.CommentRepository;
import com.a9.etutoring.repository.PostAttachmentRepository;
import com.a9.etutoring.repository.PostRepository;
import com.a9.etutoring.repository.PostTargetRepository;
import com.a9.etutoring.repository.TutorAllocationRepository;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.service.BlogService;
import com.a9.etutoring.service.EmailService;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class BlogServiceImpl implements BlogService {

    private final PostRepository postRepository;
    private final PostAttachmentRepository postAttachmentRepository;
    private final PostTargetRepository postTargetRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final TutorAllocationRepository tutorAllocationRepository;
    private final EmailService emailService;

    public BlogServiceImpl(
        PostRepository postRepository,
        PostAttachmentRepository postAttachmentRepository,
        PostTargetRepository postTargetRepository,
        CommentRepository commentRepository,
        UserRepository userRepository,
        TutorAllocationRepository tutorAllocationRepository,
        EmailService emailService
    ) {
        this.postRepository = postRepository;
        this.postAttachmentRepository = postAttachmentRepository;
        this.postTargetRepository = postTargetRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.tutorAllocationRepository = tutorAllocationRepository;
        this.emailService = emailService;
    }

    @Override
    public BlogPostResponse createPost(UUID creatorId, String body, List<UUID> targetStudentIds, List<MultipartFile> attachments) {
        User creator = findActiveUser(creatorId);
        ensureTutor(creator);
        touchInteraction(creator);

        Post post = Post.builder()
            .id(UUID.randomUUID())
            .createdBy(creator)
            .body(body.trim())
            .build();

        Post saved = postRepository.save(post);

        if (attachments != null) {
            attachments.stream()
                .filter(file -> file != null && !file.isEmpty())
                .forEach(file -> saveAttachment(saved, file));
        }

        List<UUID> targetIds = targetStudentIds;
        if (targetIds != null && !targetIds.isEmpty()) {
            targetStudents(saved.getId(), targetIds);
        } else {
            List<UUID> allocatedStudentIds = tutorAllocationRepository
                .findActiveAllocationsByTutorIdWithCurrentSchedule(creator.getId()).stream()
                .map(ta -> ta.getStudent().getId())
                .distinct()
                .toList();
            if (!allocatedStudentIds.isEmpty()) {
                targetStudents(saved.getId(), allocatedStudentIds);
            }
        }

        Post finalPost = findPost(saved.getId());
        notifyStudentsPostCreated(finalPost);
        return toPostResponse(finalPost);
    }

    @Override
    public BlogPostResponse updatePost(
        UUID postId,
        UUID actorId,
        String body,
        List<UUID> targetStudentIds,
        List<MultipartFile> attachments,
        List<UUID> keepAttachmentIds
    ) {
        if (body == null || body.isBlank()) {
            throw new BadRequestException("INVALID_BODY", "Body cannot be blank");
        }
        if (body.length() > 5000) {
            throw new BadRequestException("INVALID_BODY", "Body must be at most 5000 characters");
        }

        User actor = findActiveUser(actorId);
        ensureTutor(actor);
        touchInteraction(actor);

        Post post = findPost(postId);
        post.setBody(body.trim());
        post.setUpdatedDate(Instant.now());

        List<UUID> safeKeep = keepAttachmentIds == null ? List.of() : keepAttachmentIds.stream()
            .filter(id -> id != null)
            .toList();

        if (safeKeep.isEmpty()) {
            postAttachmentRepository.deleteAllByPost_Id(postId);
        } else {
            List<PostAttachment> existing = postAttachmentRepository.findAllByPost_Id(postId);
            List<PostAttachment> toRemove = existing.stream()
                .filter(a -> !safeKeep.contains(a.getId()))
                .toList();
            if (!toRemove.isEmpty()) {
                postAttachmentRepository.deleteAll(toRemove);
            }
        }

        if (attachments != null) {
            attachments.stream()
                .filter(file -> file != null && !file.isEmpty())
                .forEach(file -> saveAttachment(post, file));
        }

        List<UUID> targetIds = targetStudentIds;
        if (targetIds != null && !targetIds.isEmpty()) {
            targetStudents(postId, targetIds);
        }

        Post updatedPost = findPost(postId);
        notifyStudentsPostUpdated(updatedPost);
        return toPostResponse(updatedPost);
    }

    @Override
    public BlogPostResponse targetStudents(UUID postId, List<UUID> studentIds) {
        Post post = findPost(postId);
        List<UUID> requestedIds = studentIds == null ? List.of() : studentIds;
        Set<UUID> uniqueIds = new LinkedHashSet<>(requestedIds);

        List<User> students = uniqueIds.isEmpty()
            ? List.of()
            : userRepository.findAllByIdInAndDeletedDateIsNull(uniqueIds);

        if (students.size() != uniqueIds.size()) {
            throw new ResourceNotFoundException("USER_NOT_FOUND", "One or more target users were not found");
        }

        boolean hasNonStudent = students.stream().anyMatch(user -> user.getRole() != UserRole.STUDENT);
        if (hasNonStudent) {
            throw new BadRequestException("INVALID_TARGET", "Only users with STUDENT role can be targeted");
        }

        postTargetRepository.deleteAllByPost_Id(postId);

        students.forEach(student -> {
            PostTarget target = PostTarget.builder()
                .id(new PostTargetId(post.getId(), student.getId()))
                .post(post)
                .student(student)
                .build();
            postTargetRepository.save(target);
        });

        return toPostResponse(post);
    }

    @Override
    public BlogCommentResponse addComment(UUID postId, UUID authorId, BlogCreateCommentRequest request) {
        Post post = findPost(postId);
        User author = findActiveUser(authorId);
        if (author.getRole() == UserRole.STUDENT) {
            boolean targeted = postTargetRepository.existsByPost_IdAndStudent_Id(postId, authorId)
                || !postTargetRepository.existsByPost_Id(postId);
            if (!targeted) {
                throw new ForbiddenException("ACCESS_DENIED", "You do not have access to this post");
            }
        } else if (author.getRole() != UserRole.TUTOR) {
            throw new BadRequestException("ONLY_TUTORS_OR_STUDENTS_CAN_COMMENT", "Only tutors and students can comment");
        }
        touchInteraction(author);

        Comment comment = Comment.builder()
            .id(UUID.randomUUID())
            .post(post)
            .authorUser(author)
            .comment(request.comment().trim())
            .build();

        Comment saved = commentRepository.saveAndFlush(comment);
        if (author.getRole() == UserRole.STUDENT) {
            notifyTutorStudentCommented(post, saved);
        } else {
            notifyStudentsTutorCommented(post, saved);
        }
        return toCommentResponse(saved);
    }

    @Override
    public BlogCommentResponse updateComment(UUID commentId, UUID authorId, BlogCreateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("COMMENT_NOT_FOUND", "Comment not found"));

        if (!comment.getAuthorUser().getId().equals(authorId)) {
            throw new ForbiddenException("NOT_COMMENT_AUTHOR", "Only the author can edit this comment");
        }

        comment.setComment(request.comment().trim());
        comment.setUpdatedDate(Instant.now());
        Comment saved = commentRepository.save(comment);
        User author = saved.getAuthorUser();
        Post post = saved.getPost();
        if (author.getRole() == UserRole.STUDENT) {
            notifyTutorStudentCommented(post, saved);
        } else {
            notifyStudentsTutorCommented(post, saved);
        }
        return toCommentResponse(saved);
    }

    @Override
    public void deletePost(UUID postId, UUID actorId) {
        User actor = findActiveUser(actorId);
        ensureTutor(actor);
        touchInteraction(actor);

        Post post = postRepository.findByIdAndDeletedDateIsNull(postId)
            .orElseThrow(() -> new ResourceNotFoundException("POST_NOT_FOUND", "Post not found"));
        if (!post.getCreatedBy().getId().equals(actorId)) {
            throw new ForbiddenException("NOT_POST_CREATOR", "Only the tutor who created the post can delete it");
        }

        Instant now = Instant.now();
        post.setDeletedDate(now);
        post.setUpdatedDate(now);
        postRepository.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogPostResponse getPost(UUID postId, UUID requesterId) {
        Post post = findPost(postId);
        User requester = findActiveUser(requesterId);

        if (requester.getRole() == UserRole.STUDENT) {
            boolean targeted = postTargetRepository.existsByPost_IdAndStudent_Id(postId, requesterId)
                || !postTargetRepository.existsByPost_Id(postId);
            if (!targeted) {
                throw new ForbiddenException("ACCESS_DENIED", "You do not have access to this post");
            }
            return toPostResponse(post, false);
        }

        if (requester.getRole() == UserRole.TUTOR && !post.getCreatedBy().getId().equals(requesterId)) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have access to this post");
        }

        return toPostResponse(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogPostResponse> listVisiblePosts(UUID userId, UserRole role) {
        List<Post> posts;
        if (role == UserRole.STUDENT) {
            posts = postRepository.findVisibleForStudent(userId);
        } else if (role == UserRole.TUTOR) {
            posts = postRepository.findByCreatedBy_IdAndDeletedDateIsNullOrderByCreatedDateDesc(userId);
        } else {
            posts = postRepository.findAllVisibleForStaff();
        }

        boolean includeTargets = role != UserRole.STUDENT;
        return posts.stream().map(post -> toPostResponse(post, includeTargets)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadAttachment(UUID attachmentId, UUID requesterId) {
        PostAttachment attachment = postAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new ResourceNotFoundException("ATTACHMENT_NOT_FOUND", "Attachment not found"));

        Post post = attachment.getPost();
        if (post.getDeletedDate() != null) {
            throw new ResourceNotFoundException("ATTACHMENT_NOT_FOUND", "Attachment not found");
        }

        boolean isTutor = post.getCreatedBy().getId().equals(requesterId);
        boolean isTargetedStudent = postTargetRepository.existsByPost_IdAndStudent_Id(post.getId(), requesterId)
            || !postTargetRepository.existsByPost_Id(post.getId());

        if (!isTutor && !isTargetedStudent) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have access to this attachment");
        }

        byte[] data = attachment.getFileData();
        if (data == null || data.length == 0) {
            throw new ResourceNotFoundException("ATTACHMENT_NOT_FOUND", "Attachment data is unavailable");
        }

        String contentType = attachment.getContentType() != null
            ? attachment.getContentType()
            : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(
            ContentDisposition.attachment().filename(attachment.getFileName()).build()
        );

        return ResponseEntity.ok().headers(headers).body(data);
    }

    private void saveAttachment(Post post, MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BadRequestException("INVALID_ATTACHMENT", "Attachment file name cannot be blank");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("ATTACHMENT_UPLOAD_FAILED", "Unable to read attachment data");
        }

        PostAttachment attachment = PostAttachment.builder()
            .id(UUID.randomUUID())
            .post(post)
            .fileName(sanitizeFileName(originalName))
            .fileData(data)
            .contentType(file.getContentType())
            .build();

        postAttachmentRepository.save(attachment);
    }

    private Post findPost(UUID postId) {
        return postRepository.findByIdAndDeletedDateIsNull(postId)
            .orElseThrow(() -> new ResourceNotFoundException("POST_NOT_FOUND", "Post not found"));
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedDateIsNull(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private void ensureTutor(User user) {
        if (user.getRole() != UserRole.TUTOR) {
            throw new BadRequestException("ONLY_TUTORS_CAN_POST", "Only tutors can create or manage blog posts");
        }
    }

    private BlogPostResponse toPostResponse(Post post) {
        return toPostResponse(post, true);
    }

    private BlogPostResponse toPostResponse(Post post, boolean includeTargets) {
        List<AttachmentResponse> attachments = postAttachmentRepository.findProjectedByPost_Id(post.getId()).stream()
            .map(a -> new AttachmentResponse(a.getId(), a.getFileName()))
            .toList();

        List<UUID> targetStudentIds = includeTargets
            ? postTargetRepository.findAllByPost_Id(post.getId()).stream()
            .map(target -> target.getStudent().getId())
            .toList()
            : List.of();

        List<BlogCommentResponse> comments = commentRepository.findAllVisibleByPostIdOrderByCreatedDateAsc(post.getId()).stream()
            .map(this::toCommentResponse)
            .toList();

        return new BlogPostResponse(
            post.getId(),
            post.getCreatedBy().getId(),
            post.getBody(),
            post.getCreatedDate(),
            post.getUpdatedDate(),
            attachments,
            targetStudentIds,
            comments
        );
    }

    private BlogCommentResponse toCommentResponse(Comment comment) {
        return new BlogCommentResponse(
            comment.getId(),
            comment.getPost().getId(),
            comment.getAuthorUser().getId(),
            comment.getComment(),
            comment.getCreatedDate(),
            comment.getUpdatedDate()
        );
    }

    private void notifyStudentsPostCreated(Post post) {
        String tutorName = buildFullName(post.getCreatedBy());
        String preview = post.getBody().length() > 100 ? post.getBody().substring(0, 100) + "..." : post.getBody();
        String body = String.format(
            "Dear %s,\n\n%s has published a new blog post:\n\n\"%s\"\n\nBest regards,\neTutoring System",
            "%s", tutorName, preview);
        postTargetRepository.findAllByPost_Id(post.getId()).forEach(target -> {
            User student = target.getStudent();
            emailService.sendEmail(
                student.getEmail(),
                "New blog post – eTutoring",
                String.format(body, buildFullName(student))
            );
        });
    }

    private void notifyStudentsPostUpdated(Post post) {
        String tutorName = buildFullName(post.getCreatedBy());
        String preview = post.getBody().length() > 100 ? post.getBody().substring(0, 100) + "..." : post.getBody();
        String body = String.format(
            "Dear %s,\n\n%s has updated a blog post:\n\n\"%s\"\n\nBest regards,\neTutoring System",
            "%s", tutorName, preview);
        postTargetRepository.findAllByPost_Id(post.getId()).forEach(target -> {
            User student = target.getStudent();
            emailService.sendEmail(
                student.getEmail(),
                "Blog post updated – eTutoring",
                String.format(body, buildFullName(student))
            );
        });
    }

    private void notifyStudentsTutorCommented(Post post, Comment comment) {
        String tutorName = buildFullName(comment.getAuthorUser());
        String body = String.format(
            "Dear %s,\n\n%s has commented on a blog post:\n\n\"%s\"\n\nBest regards,\neTutoring System",
            "%s", tutorName, comment.getComment());
        postTargetRepository.findAllByPost_Id(post.getId()).forEach(target -> {
            User student = target.getStudent();
            emailService.sendEmail(
                student.getEmail(),
                "New comment on a blog post – eTutoring",
                String.format(body, buildFullName(student))
            );
        });
    }

    private void notifyTutorStudentCommented(Post post, Comment comment) {
        User tutor = post.getCreatedBy();
        String studentName = buildFullName(comment.getAuthorUser());
        String body = String.format(
            "Dear %s,\n\n%s has commented on your blog post:\n\n\"%s\"\n\nBest regards,\neTutoring System",
            buildFullName(tutor), studentName, comment.getComment());
        emailService.sendEmail(tutor.getEmail(), "A student commented on your blog post – eTutoring", body);
    }

    private String buildFullName(User user) {
        if (user.getFirstName() != null && !user.getFirstName().isBlank()
            && user.getLastName() != null && !user.getLastName().isBlank()) {
            return user.getFirstName() + " " + user.getLastName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }

    private void touchInteraction(User user) {
        user.setLastInteractionDate(Instant.now());
        userRepository.save(user);
    }

    private String sanitizeFileName(String fileName) {
        String sanitized = fileName.replace("\\", "_").replace("/", "_").trim();
        if (sanitized.isBlank()) {
            throw new BadRequestException("INVALID_ATTACHMENT", "Attachment file name cannot be blank");
        }
        return sanitized.length() > 255 ? sanitized.substring(0, 255) : sanitized;
    }
}
