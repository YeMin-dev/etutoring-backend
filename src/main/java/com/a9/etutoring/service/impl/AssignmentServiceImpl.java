package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.dto.assignment.AssignmentFeedbackRequest;
import com.a9.etutoring.domain.dto.assignment.AssignmentResponse;
import com.a9.etutoring.domain.dto.assignment.AssignmentSubmissionResponse;
import com.a9.etutoring.domain.dto.assignment.AssignmentSubmissionSummaryResponse;
import com.a9.etutoring.domain.dto.assignment.AssignmentSummaryResponse;
import com.a9.etutoring.domain.dto.blog.AttachmentResponse;
import com.a9.etutoring.domain.enums.AssignmentSubmissionStatus;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.Assignment;
import com.a9.etutoring.domain.model.AssignmentAttachment;
import com.a9.etutoring.domain.model.AssignmentSubmission;
import com.a9.etutoring.domain.model.SubmissionAttachment;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.exception.ForbiddenException;
import com.a9.etutoring.exception.ResourceNotFoundException;
import com.a9.etutoring.repository.AssignmentAttachmentRepository;
import com.a9.etutoring.repository.AssignmentRepository;
import com.a9.etutoring.repository.AssignmentSubmissionRepository;
import com.a9.etutoring.repository.SubmissionAttachmentRepository;
import com.a9.etutoring.repository.TutorAllocationRepository;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.config.AppProperties;
import com.a9.etutoring.service.AssignmentService;
import com.a9.etutoring.service.EmailService;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class AssignmentServiceImpl implements AssignmentService {

    private static final DateTimeFormatter ASSIGNMENT_DUE_DATE_INPUT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AssignmentRepository assignmentRepository;
    private final AssignmentAttachmentRepository assignmentAttachmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final UserRepository userRepository;
    private final TutorAllocationRepository tutorAllocationRepository;
    private final EmailService emailService;
    private final AppProperties appProperties;

    public AssignmentServiceImpl(
        AssignmentRepository assignmentRepository,
        AssignmentAttachmentRepository assignmentAttachmentRepository,
        AssignmentSubmissionRepository assignmentSubmissionRepository,
        SubmissionAttachmentRepository submissionAttachmentRepository,
        UserRepository userRepository,
        TutorAllocationRepository tutorAllocationRepository,
        EmailService emailService,
        AppProperties appProperties
    ) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentAttachmentRepository = assignmentAttachmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.submissionAttachmentRepository = submissionAttachmentRepository;
        this.userRepository = userRepository;
        this.tutorAllocationRepository = tutorAllocationRepository;
        this.emailService = emailService;
        this.appProperties = appProperties;
    }

    @Override
    public AssignmentResponse createAssignment(
        UUID tutorId,
        String title,
        String instructions,
        String dueDate,
        List<MultipartFile> attachments
    ) {
        User tutor = findActiveUser(tutorId);
        ensureTutor(tutor);

        if (title == null || title.isBlank()) {
            throw new BadRequestException("INVALID_TITLE", "Title cannot be blank");
        }
        if (instructions == null || instructions.isBlank()) {
            throw new BadRequestException("INVALID_INSTRUCTIONS", "Instructions cannot be blank");
        }
        if (title.length() > 255) {
            throw new BadRequestException("INVALID_TITLE", "Title must be at most 255 characters");
        }
        if (instructions.length() > 10000) {
            throw new BadRequestException("INVALID_INSTRUCTIONS", "Instructions must be at most 10000 characters");
        }

        Assignment assignment = Assignment.builder()
            .id(UUID.randomUUID())
            .createdBy(tutor)
            .title(title.trim())
            .instructions(instructions.trim())
            .dueDate(parseDueDate(dueDate))
            .build();

        Assignment saved = assignmentRepository.save(assignment);
        if (attachments != null) {
            attachments.stream()
                .filter(f -> f != null && !f.isEmpty())
                .forEach(f -> saveAssignmentAttachment(saved, f));
        }

        Assignment created = findAssignment(saved.getId());
        notifyStudentsAssignmentCreated(created);
        return toAssignmentResponse(created, List.of());
    }

    @Override
    public AssignmentResponse updateAssignment(
        UUID assignmentId,
        UUID tutorId,
        String title,
        String instructions,
        String dueDate,
        List<UUID> keepAttachmentIds,
        List<MultipartFile> attachments
    ) {
        User tutor = findActiveUser(tutorId);
        ensureTutor(tutor);

        Assignment assignment = findOwnedAssignment(assignmentId, tutorId);

        if (title == null || title.isBlank()) {
            throw new BadRequestException("INVALID_TITLE", "Title cannot be blank");
        }
        if (instructions == null || instructions.isBlank()) {
            throw new BadRequestException("INVALID_INSTRUCTIONS", "Instructions cannot be blank");
        }
        if (title.length() > 255 || instructions.length() > 10000) {
            throw new BadRequestException("INVALID_FIELDS", "Title or instructions exceed maximum length");
        }

        assignment.setTitle(title.trim());
        assignment.setInstructions(instructions.trim());
        assignment.setDueDate(parseDueDate(dueDate));
        assignment.setUpdatedDate(Instant.now());
        assignmentRepository.save(assignment);

        List<UUID> safeKeep = keepAttachmentIds == null ? List.of() : keepAttachmentIds.stream()
            .filter(id -> id != null)
            .toList();

        if (safeKeep.isEmpty()) {
            assignmentAttachmentRepository.deleteAllByAssignment_Id(assignmentId);
        } else {
            List<AssignmentAttachment> existing = assignmentAttachmentRepository.findAllByAssignment_Id(assignmentId);
            List<AssignmentAttachment> toRemove = existing.stream()
                .filter(a -> !safeKeep.contains(a.getId()))
                .toList();
            if (!toRemove.isEmpty()) {
                assignmentAttachmentRepository.deleteAll(toRemove);
            }
        }

        if (attachments != null) {
            attachments.stream()
                .filter(f -> f != null && !f.isEmpty())
                .forEach(f -> saveAssignmentAttachment(assignment, f));
        }

        Assignment updated = findAssignment(assignmentId);
        notifyStudentsAssignmentUpdated(updated);
        return toAssignmentResponse(updated, List.of());
    }

    @Override
    public void deleteAssignment(UUID assignmentId, UUID tutorId) {
        User tutor = findActiveUser(tutorId);
        ensureTutor(tutor);
        Assignment assignment = findOwnedAssignment(assignmentId, tutorId);
        String title = assignment.getTitle();
        UUID tutorUserId = tutor.getId();
        assignment.setDeletedDate(Instant.now());
        assignment.setUpdatedDate(Instant.now());
        assignmentRepository.save(assignment);
        notifyStudentsAssignmentDeleted(tutorUserId, title);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSummaryResponse> listForTutor(UUID tutorId) {
        findActiveUser(tutorId);
        return assignmentRepository.findByCreatedBy_IdAndDeletedDateIsNullOrderByCreatedDateDesc(tutorId).stream()
            .map(this::toSummaryResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getForTutor(UUID assignmentId, UUID tutorId) {
        Assignment assignment = findOwnedAssignment(assignmentId, tutorId);
        List<AssignmentSubmissionSummaryResponse> subs = assignmentSubmissionRepository
            .findAllByAssignment_IdOrderBySubmittedAtAsc(assignmentId).stream()
            .map(this::toSubmissionSummaryResponse)
            .toList();
        return toAssignmentResponse(assignment, subs);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentSubmissionResponse getSubmissionForTutor(UUID assignmentId, UUID submissionId, UUID tutorId) {
        findOwnedAssignment(assignmentId, tutorId);
        AssignmentSubmission sub = assignmentSubmissionRepository.findByIdAndAssignment_Id(submissionId, assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND", "Submission not found"));
        return toSubmissionResponse(sub);
    }

    @Override
    public void setFeedback(UUID assignmentId, UUID submissionId, UUID tutorId, AssignmentFeedbackRequest request) {
        User tutor = findActiveUser(tutorId);
        ensureTutor(tutor);
        findOwnedAssignment(assignmentId, tutorId);
        AssignmentSubmission sub = assignmentSubmissionRepository.findByIdAndAssignment_Id(submissionId, assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND", "Submission not found"));

        sub.setFeedbackText(request.feedbackText().trim());
        sub.setFeedbackAt(Instant.now());
        sub.setFeedbackBy(tutor);
        assignmentSubmissionRepository.save(sub);
        notifyStudentFeedback(sub, tutor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSummaryResponse> listForStudent(UUID studentId) {
        User student = findActiveUser(studentId);
        ensureStudent(student);
        return assignmentRepository.findVisibleForStudent(studentId).stream()
            .map(this::toSummaryResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getForStudent(UUID assignmentId, UUID studentId) {
        User student = findActiveUser(studentId);
        ensureStudent(student);
        Assignment assignment = findAssignmentVisibleToStudent(assignmentId, studentId);
        List<AssignmentSubmissionSummaryResponse> subs = assignmentSubmissionRepository
            .findByAssignment_IdAndStudent_Id(assignmentId, studentId)
            .map(s -> List.of(toSubmissionSummaryResponse(s)))
            .orElse(List.of());
        return toAssignmentResponse(assignment, subs);
    }

    @Override
    public AssignmentSubmissionResponse getOrCreateSubmissionForStudent(UUID assignmentId, UUID studentId) {
        User student = findActiveUser(studentId);
        ensureStudent(student);
        Assignment assignment = findAssignmentVisibleToStudent(assignmentId, studentId);

        return assignmentSubmissionRepository.findByAssignment_IdAndStudent_Id(assignmentId, studentId)
            .map(this::toSubmissionResponse)
            .orElseGet(() -> {
                AssignmentSubmission draft = AssignmentSubmission.builder()
                    .id(UUID.randomUUID())
                    .assignment(assignment)
                    .student(student)
                    .submittedAt(Instant.now())
                    .status(AssignmentSubmissionStatus.DRAFT)
                    .build();
                return toSubmissionResponse(assignmentSubmissionRepository.save(draft));
            });
    }

    @Override
    public AssignmentSubmissionResponse submitAssignment(UUID assignmentId, UUID studentId, List<MultipartFile> files) {
        User student = findActiveUser(studentId);
        ensureStudent(student);
        Assignment assignment = findAssignmentVisibleToStudent(assignmentId, studentId);

        List<MultipartFile> nonEmpty = files == null ? List.of() : files.stream()
            .filter(f -> f != null && !f.isEmpty())
            .toList();
        if (nonEmpty.isEmpty()) {
            throw new BadRequestException("INVALID_SUBMISSION", "At least one attachment file is required");
        }

        Optional<AssignmentSubmission> existingOpt = assignmentSubmissionRepository
            .findByAssignment_IdAndStudent_Id(assignmentId, studentId);

        boolean resubmit = existingOpt.isPresent()
            && existingOpt.get().getStatus() == AssignmentSubmissionStatus.SUBMITTED;

        AssignmentSubmission submission;
        if (existingOpt.isPresent()) {
            submission = existingOpt.get();
            submissionAttachmentRepository.deleteAllBySubmission_Id(submission.getId());
            submission.getAttachments().clear();
        } else {
            submission = AssignmentSubmission.builder()
                .id(UUID.randomUUID())
                .assignment(assignment)
                .student(student)
                .submittedAt(Instant.now())
                .status(AssignmentSubmissionStatus.DRAFT)
                .build();
        }

        Instant now = Instant.now();
        submission.setSubmittedAt(now);
        submission.setUpdatedAt(now);
        submission.setStatus(AssignmentSubmissionStatus.SUBMITTED);
        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);

        for (MultipartFile file : nonEmpty) {
            saveSubmissionAttachment(saved, file);
        }

        AssignmentSubmission persisted = assignmentSubmissionRepository.findById(saved.getId()).orElseThrow();
        notifyTutorSubmissionActivity(persisted, assignment, resubmit);
        return toSubmissionResponse(persisted);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadAssignmentAttachment(UUID attachmentId, UUID userId, UserRole role) {
        AssignmentAttachment attachment = assignmentAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new ResourceNotFoundException("ATTACHMENT_NOT_FOUND", "Attachment not found"));

        Assignment assignment = attachment.getAssignment();
        if (assignment.getDeletedDate() != null) {
            throw new ResourceNotFoundException("ATTACHMENT_NOT_FOUND", "Attachment not found");
        }

        UUID tutorId = assignment.getCreatedBy().getId();
        if (role == UserRole.TUTOR && !tutorId.equals(userId)) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have access to this attachment");
        }
        if (role == UserRole.STUDENT) {
            if (tutorAllocationRepository.countActiveAllocationsForStudentAndTutor(userId, tutorId) == 0) {
                throw new ForbiddenException("ACCESS_DENIED", "You do not have access to this attachment");
            }
        } else if (role != UserRole.TUTOR && role != UserRole.ADMIN) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have access to this attachment");
        }

        return buildFileResponse(attachment.getFileData(), attachment.getContentType(), attachment.getFileName());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadSubmissionAttachment(UUID attachmentId, UUID userId, UserRole role) {
        SubmissionAttachment attachment = submissionAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new ResourceNotFoundException("ATTACHMENT_NOT_FOUND", "Attachment not found"));

        AssignmentSubmission submission = attachment.getSubmission();
        Assignment assignment = submission.getAssignment();
        if (assignment.getDeletedDate() != null) {
            throw new ResourceNotFoundException("ATTACHMENT_NOT_FOUND", "Attachment not found");
        }

        UUID tutorId = assignment.getCreatedBy().getId();
        UUID studentId = submission.getStudent().getId();

        if (role == UserRole.STUDENT && !studentId.equals(userId)) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have access to this attachment");
        }
        if (role == UserRole.TUTOR && !tutorId.equals(userId)) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have access to this attachment");
        }
        if (role != UserRole.STUDENT && role != UserRole.TUTOR && role != UserRole.ADMIN) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have access to this attachment");
        }

        return buildFileResponse(attachment.getFileData(), attachment.getContentType(), attachment.getFileName());
    }

    private ResponseEntity<byte[]> buildFileResponse(byte[] data, String contentType, String fileName) {
        if (data == null || data.length == 0) {
            throw new ResourceNotFoundException("ATTACHMENT_NOT_FOUND", "Attachment data is unavailable");
        }
        String ct = contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(ct));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

    private void saveAssignmentAttachment(Assignment assignment, MultipartFile file) {
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
        AssignmentAttachment att = AssignmentAttachment.builder()
            .id(UUID.randomUUID())
            .assignment(assignment)
            .fileName(sanitizeFileName(originalName))
            .fileData(data)
            .contentType(file.getContentType())
            .build();
        assignmentAttachmentRepository.save(att);
    }

    private void saveSubmissionAttachment(AssignmentSubmission submission, MultipartFile file) {
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
        SubmissionAttachment att = SubmissionAttachment.builder()
            .id(UUID.randomUUID())
            .submission(submission)
            .fileName(sanitizeFileName(originalName))
            .fileData(data)
            .contentType(file.getContentType())
            .build();
        submissionAttachmentRepository.save(att);
    }

    private Assignment findAssignment(UUID id) {
        return assignmentRepository.findByIdAndDeletedDateIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("ASSIGNMENT_NOT_FOUND", "Assignment not found"));
    }

    private Assignment findOwnedAssignment(UUID assignmentId, UUID tutorId) {
        Assignment a = findAssignment(assignmentId);
        if (!a.getCreatedBy().getId().equals(tutorId)) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have access to this assignment");
        }
        return a;
    }

    private Assignment findAssignmentVisibleToStudent(UUID assignmentId, UUID studentId) {
        Assignment a = findAssignment(assignmentId);
        UUID tutorId = a.getCreatedBy().getId();
        if (tutorAllocationRepository.countActiveAllocationsForStudentAndTutor(studentId, tutorId) == 0) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have access to this assignment");
        }
        return a;
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedDateIsNull(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private void ensureTutor(User user) {
        if (user.getRole() != UserRole.TUTOR) {
            throw new BadRequestException("ONLY_TUTORS", "Only tutors can manage assignments");
        }
    }

    private void ensureStudent(User user) {
        if (user.getRole() != UserRole.STUDENT) {
            throw new BadRequestException("ONLY_STUDENTS", "Only students can access student assignment endpoints");
        }
    }

    private Instant parseDueDate(String dueDateText) {
        if (dueDateText == null || dueDateText.isBlank()) {
            return null;
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(dueDateText.trim(), ASSIGNMENT_DUE_DATE_INPUT);
            return ldt.atZone(appProperties.getDefaultTimeZone()).toInstant();
        } catch (DateTimeParseException ex) {
            throw new BadRequestException(
                "INVALID_DUE_DATE",
                "dueDate must be dd/MM/yyyy HH:mm in 24-hour time (e.g. 31/12/2026 23:59), using app.default-time-zone"
            );
        }
    }

    private AssignmentSummaryResponse toSummaryResponse(Assignment a) {
        return new AssignmentSummaryResponse(
            a.getId(),
            a.getCreatedBy().getId(),
            a.getTitle(),
            a.getDueDate(),
            a.getCreatedDate(),
            a.getUpdatedDate()
        );
    }

    private AssignmentResponse toAssignmentResponse(Assignment a, List<AssignmentSubmissionSummaryResponse> submissions) {
        List<AttachmentResponse> attachments = assignmentAttachmentRepository.findByAssignment_Id(a.getId()).stream()
            .map(x -> new AttachmentResponse(x.getId(), x.getFileName()))
            .toList();
        return new AssignmentResponse(
            a.getId(),
            a.getCreatedBy().getId(),
            a.getTitle(),
            a.getInstructions(),
            a.getDueDate(),
            a.getCreatedDate(),
            a.getUpdatedDate(),
            attachments,
            submissions
        );
    }

    private AssignmentSubmissionSummaryResponse toSubmissionSummaryResponse(AssignmentSubmission s) {
        List<AttachmentResponse> submissionAttachments = submissionAttachmentRepository.findBySubmission_Id(s.getId()).stream()
            .map(x -> new AttachmentResponse(x.getId(), x.getFileName()))
            .toList();
        return new AssignmentSubmissionSummaryResponse(
            s.getId(),
            s.getStudent().getId(),
            s.getStatus(),
            s.getSubmittedAt(),
            s.getUpdatedAt(),
            s.getFeedbackText() != null && !s.getFeedbackText().isBlank(),
            submissionAttachments
        );
    }

    private AssignmentSubmissionResponse toSubmissionResponse(AssignmentSubmission s) {
        List<AttachmentResponse> attachments = submissionAttachmentRepository.findBySubmission_Id(s.getId()).stream()
            .map(x -> new AttachmentResponse(x.getId(), x.getFileName()))
            .toList();
        return new AssignmentSubmissionResponse(
            s.getId(),
            s.getAssignment().getId(),
            s.getStudent().getId(),
            s.getStatus(),
            s.getSubmittedAt(),
            s.getUpdatedAt(),
            s.getFeedbackText(),
            s.getFeedbackAt(),
            s.getFeedbackBy() != null ? s.getFeedbackBy().getId() : null,
            attachments
        );
    }

    private String sanitizeFileName(String fileName) {
        String sanitized = fileName.replace("\\", "_").replace("/", "_").trim();
        if (sanitized.isBlank()) {
            throw new BadRequestException("INVALID_ATTACHMENT", "Attachment file name cannot be blank");
        }
        return sanitized.length() > 255 ? sanitized.substring(0, 255) : sanitized;
    }

    private List<User> collectActiveAllocatedStudents(UUID tutorId) {
        Map<UUID, User> byId = new LinkedHashMap<>();
        for (var ta : tutorAllocationRepository.findActiveAllocationsByTutorIdWithCurrentSchedule(tutorId)) {
            User st = ta.getStudent();
            if (st != null) {
                byId.putIfAbsent(st.getId(), st);
            }
        }
        return List.copyOf(byId.values());
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

    private void notifyStudentsAssignmentCreated(Assignment assignment) {
        User tutor = assignment.getCreatedBy();
        String tutorName = buildFullName(tutor);
        String bodyTemplate = "Dear %s,\n\n%s has posted a new assignment: \"%s\".\n\n"
            + "Log in to eTutoring to view the instructions and requirements.\n\n"
            + "Best regards,\neTutoring System";
        collectActiveAllocatedStudents(tutor.getId()).forEach(student -> {
            String body = String.format(bodyTemplate, buildFullName(student), tutorName, assignment.getTitle());
            emailService.sendEmail(student.getEmail(), "New assignment – eTutoring", body);
        });
    }

    private void notifyStudentsAssignmentUpdated(Assignment assignment) {
        User tutor = assignment.getCreatedBy();
        String tutorName = buildFullName(tutor);
        String bodyTemplate = "Dear %s,\n\n%s has updated an assignment: \"%s\".\n\n"
            + "Log in to eTutoring to review the latest details.\n\n"
            + "Best regards,\neTutoring System";
        collectActiveAllocatedStudents(tutor.getId()).forEach(student -> {
            String body = String.format(bodyTemplate, buildFullName(student), tutorName, assignment.getTitle());
            emailService.sendEmail(student.getEmail(), "Assignment updated – eTutoring", body);
        });
    }

    private void notifyStudentsAssignmentDeleted(UUID tutorId, String assignmentTitle) {
        User tutor = userRepository.findByIdAndDeletedDateIsNull(tutorId).orElse(null);
        String tutorName = tutor != null ? buildFullName(tutor) : "Your tutor";
        String bodyTemplate = "Dear %s,\n\n%s has removed the assignment: \"%s\".\n\n"
            + "It will no longer appear in your assignment list.\n\n"
            + "Best regards,\neTutoring System";
        collectActiveAllocatedStudents(tutorId).forEach(student -> {
            String body = String.format(bodyTemplate, buildFullName(student), tutorName, assignmentTitle);
            emailService.sendEmail(student.getEmail(), "Assignment removed – eTutoring", body);
        });
    }

    private void notifyTutorSubmissionActivity(AssignmentSubmission submission, Assignment assignment, boolean resubmit) {
        User tutor = assignment.getCreatedBy();
        String studentName = buildFullName(submission.getStudent());
        String title = assignment.getTitle();
        String subject = resubmit ? "Assignment resubmitted – eTutoring" : "New assignment submission – eTutoring";
        String action = resubmit ? "has resubmitted work for" : "has submitted work for";
        String body = String.format(
            "Dear %s,\n\n%s %s the assignment \"%s\".\n\n"
                + "Log in to eTutoring to review the submission.\n\n"
                + "Best regards,\neTutoring System",
            buildFullName(tutor), studentName, action, title);
        emailService.sendEmail(tutor.getEmail(), subject, body);
    }

    private void notifyStudentFeedback(AssignmentSubmission submission, User tutor) {
        User student = submission.getStudent();
        String assignmentTitle = submission.getAssignment().getTitle();
        String preview = submission.getFeedbackText().length() > 200
            ? submission.getFeedbackText().substring(0, 200) + "..."
            : submission.getFeedbackText();
        String body = String.format(
            "Dear %s,\n\n%s has left feedback on your submission for \"%s\":\n\n\"%s\"\n\n"
                + "Log in to eTutoring to read the full feedback.\n\n"
                + "Best regards,\neTutoring System",
            buildFullName(student), buildFullName(tutor), assignmentTitle, preview);
        emailService.sendEmail(student.getEmail(), "Feedback on your assignment – eTutoring", body);
    }
}
