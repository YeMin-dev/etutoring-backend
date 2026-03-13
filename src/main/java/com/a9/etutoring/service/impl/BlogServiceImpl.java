package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.dto.blog.BlogCommentResponse;
import com.a9.etutoring.domain.dto.blog.BlogCreateCommentRequest;
import com.a9.etutoring.domain.dto.blog.BlogCreatePostRequest;
import com.a9.etutoring.domain.dto.blog.BlogPostResponse;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.Comment;
import com.a9.etutoring.domain.model.Post;
import com.a9.etutoring.domain.model.PostAttachment;
import com.a9.etutoring.domain.model.PostTarget;
import com.a9.etutoring.domain.model.PostTargetId;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.exception.ResourceNotFoundException;
import com.a9.etutoring.repository.CommentRepository;
import com.a9.etutoring.repository.PostAttachmentRepository;
import com.a9.etutoring.repository.PostRepository;
import com.a9.etutoring.repository.PostTargetRepository;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.service.BlogService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class BlogServiceImpl implements BlogService {

    private static final Path BLOG_UPLOAD_ROOT = Paths.get("uploads", "blog");

    private final PostRepository postRepository;
    private final PostAttachmentRepository postAttachmentRepository;
    private final PostTargetRepository postTargetRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public BlogServiceImpl(
        PostRepository postRepository,
        PostAttachmentRepository postAttachmentRepository,
        PostTargetRepository postTargetRepository,
        CommentRepository commentRepository,
        UserRepository userRepository
    ) {
        this.postRepository = postRepository;
        this.postAttachmentRepository = postAttachmentRepository;
        this.postTargetRepository = postTargetRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public BlogPostResponse createPost(UUID creatorId, BlogCreatePostRequest request) {
        User creator = findActiveUser(creatorId);
        ensureTutorOrAdmin(creator);
        touchInteraction(creator);

        Post post = Post.builder()
            .id(UUID.randomUUID())
            .createdBy(creator)
            .body(request.body().trim())
            .build();

        Post saved = postRepository.save(post);

        if (request.attachments() != null) {
            request.attachments().stream()
                .filter(fileName -> fileName != null && !fileName.isBlank())
                .forEach(fileName -> addAttachment(saved.getId(), creatorId, fileName));
        }

        if (request.targetStudentIds() != null) {
            targetStudents(saved.getId(), request.targetStudentIds());
        }

        return toPostResponse(findPost(saved.getId()));
    }

    @Override
    public BlogPostResponse addAttachment(UUID postId, UUID actorId, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException("INVALID_ATTACHMENT", "Attachment file name cannot be blank");
        }

        User actor = findActiveUser(actorId);
        ensureTutorOrAdmin(actor);
        touchInteraction(actor);

        Post post = findPost(postId);
        PostAttachment attachment = PostAttachment.builder()
            .id(UUID.randomUUID())
            .post(post)
            .fileName(fileName.trim())
            .build();

        postAttachmentRepository.save(attachment);
        return toPostResponse(post);
    }

    @Override
    public BlogPostResponse uploadAttachment(UUID postId, UUID actorId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("INVALID_ATTACHMENT", "Attachment file cannot be empty");
        }

        User actor = findActiveUser(actorId);
        ensureTutorOrAdmin(actor);
        touchInteraction(actor);

        Post post = findPost(postId);
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BadRequestException("INVALID_ATTACHMENT", "Attachment file name cannot be blank");
        }

        String safeName = sanitizeFileName(originalName);
        String storedName = UUID.randomUUID() + "_" + safeName;

        try {
            Path postDir = BLOG_UPLOAD_ROOT.resolve(postId.toString());
            Files.createDirectories(postDir);
            Files.copy(file.getInputStream(), postDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BadRequestException("ATTACHMENT_UPLOAD_FAILED", "Unable to store attachment");
        }

        PostAttachment attachment = PostAttachment.builder()
            .id(UUID.randomUUID())
            .post(post)
            .fileName(storedName)
            .build();

        postAttachmentRepository.save(attachment);
        return toPostResponse(post);
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
        touchInteraction(author);

        if (author.getRole() == UserRole.STUDENT && !isVisibleToStudent(postId, authorId)) {
            throw new ResourceNotFoundException("POST_NOT_FOUND", "Post not found");
        }

        PostAttachment attachment = null;
        if (request.attachmentId() != null) {
            attachment = postAttachmentRepository.findByIdAndPost_Id(request.attachmentId(), postId)
                .orElseThrow(() -> new ResourceNotFoundException("ATTACHMENT_NOT_FOUND", "Attachment not found"));
        }

        Comment comment = Comment.builder()
            .id(UUID.randomUUID())
            .post(post)
            .authorUser(author)
            .attachment(attachment)
            .comment(request.comment().trim())
            .build();

        Comment saved = commentRepository.save(comment);
        return toCommentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogPostResponse> listVisiblePosts(UUID userId, UserRole role) {
        List<Post> posts = role == UserRole.STUDENT
            ? postRepository.findVisibleForStudent(userId)
            : postRepository.findAllVisibleForStaff();

        boolean includeTargets = role != UserRole.STUDENT;
        return posts.stream().map(post -> toPostResponse(post, includeTargets)).toList();
    }

    private boolean isVisibleToStudent(UUID postId, UUID studentId) {
        return !postTargetRepository.existsByPost_Id(postId)
            || postTargetRepository.existsByPost_IdAndStudent_Id(postId, studentId);
    }

    private Post findPost(UUID postId) {
        return postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("POST_NOT_FOUND", "Post not found"));
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedDateIsNull(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private BlogPostResponse toPostResponse(Post post) {
        return toPostResponse(post, true);
    }

    private BlogPostResponse toPostResponse(Post post, boolean includeTargets) {
        List<String> attachments = postAttachmentRepository.findAllByPost_Id(post.getId()).stream()
            .map(PostAttachment::getFileName)
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
            comment.getAttachment() != null ? comment.getAttachment().getId() : null,
            comment.getComment(),
            comment.getCreatedDate(),
            comment.getUpdatedDate()
        );
    }

    private void ensureTutorOrAdmin(User user) {
        if (user.getRole() != UserRole.TUTOR && user.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("FORBIDDEN_OPERATION", "Only tutors or admins can create posts and upload attachments");
        }
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
