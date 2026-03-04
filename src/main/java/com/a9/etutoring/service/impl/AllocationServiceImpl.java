package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.Allocation;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.exception.ResourceNotFoundException;
import com.a9.etutoring.repository.AllocationRepository;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.service.AllocationService;
import com.a9.etutoring.service.EmailService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AllocationServiceImpl implements AllocationService {

    private static final Logger logger = LoggerFactory.getLogger(AllocationServiceImpl.class);

    private final AllocationRepository allocationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public AllocationServiceImpl(AllocationRepository allocationRepository,
                                 UserRepository userRepository,
                                 EmailService emailService) {
        this.allocationRepository = allocationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    public void allocateTutor(UUID studentId, UUID tutorId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("STUDENT_NOT_FOUND", "Student not found"));
        User tutor = userRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("TUTOR_NOT_FOUND", "Tutor not found"));

        if (student.getRole() != UserRole.STUDENT) {
            throw new BadRequestException("INVALID_ROLE", "User is not a student");
        }
        if (tutor.getRole() != UserRole.TUTOR) {
            throw new BadRequestException("INVALID_ROLE", "User is not a tutor");
        }

        // Check for existing active allocation and soft delete it
        Optional<Allocation> existingAllocation = allocationRepository.findByStudentIdAndDeletedDateIsNull(studentId);
        if (existingAllocation.isPresent()) {
            Allocation oldAllocation = existingAllocation.get();
            oldAllocation.setDeletedDate(Instant.now());
            allocationRepository.save(oldAllocation);
            logger.info("Soft deleted previous allocation for studentId={} with tutorId={}", studentId, oldAllocation.getTutor().getId());
        }

        Allocation allocation = new Allocation(UUID.randomUUID(), student, tutor, Instant.now());
        allocationRepository.save(allocation);
        logger.info("Saved new allocation to database: allocationId={}, studentId={}, tutorId={}", allocation.getId(), studentId, tutorId);

        // Send emails asynchronously
        String studentFullName = buildFullName(student);
        String tutorFullName = buildFullName(tutor);

        logger.info("Triggering email notifications for allocation: studentId={}, tutorId={}, studentEmail={}, tutorEmail={}",
                studentId, tutorId, student.getEmail(), tutor.getEmail());

        emailService.sendEmail(student.getEmail(),
                buildStudentEmailSubject(),
                buildStudentEmailBody(studentFullName, tutorFullName));

        emailService.sendEmail(tutor.getEmail(),
                buildTutorEmailSubject(),
                buildTutorEmailBody(tutorFullName, studentFullName));
    }

    private String buildFullName(User user) {
        if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty() &&
            user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
            return user.getFirstName() + " " + user.getLastName();
        } else if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            return user.getUsername();
        } else {
            return user.getEmail(); // fallback
        }
    }

    private String buildStudentEmailSubject() {
        return "Personal Tutor Allocation – eTutoring";
    }

    private String buildStudentEmailBody(String studentFullName, String tutorFullName) {
        return String.format(
            "Hello %s,\n\n" +
            "You have been assigned a personal tutor:\n" +
            "Tutor: %s\n\n" +
            "Please log in to eTutoring to send messages, arrange meetings, and upload documents.\n\n" +
            "Best regards,\n" +
            "eTutoring System",
            studentFullName, tutorFullName
        );
    }

    private String buildTutorEmailSubject() {
        return "New Student Assigned – eTutoring";
    }

    private String buildTutorEmailBody(String tutorFullName, String studentFullName) {
        return String.format(
            "Hello %s,\n\n" +
            "You have been assigned a new student:\n" +
            "Student: %s\n\n" +
            "Please log in to eTutoring to view their dashboard and start communication.\n\n" +
            "Best regards,\n" +
            "eTutoring System",
            tutorFullName, studentFullName
        );
    }
}