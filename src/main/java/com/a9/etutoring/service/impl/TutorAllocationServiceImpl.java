package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.dto.allocation.AllocationCreateRequest;
import com.a9.etutoring.domain.dto.allocation.AllocationPreviewItemResponse;
import com.a9.etutoring.domain.dto.allocation.AllocationPreviewRequest;
import com.a9.etutoring.domain.dto.allocation.AllocationUpdateRequest;
import com.a9.etutoring.domain.dto.allocation.BulkAllocationPreviewResponse;
import com.a9.etutoring.domain.dto.allocation.BulkAllocationRequest;
import com.a9.etutoring.domain.dto.allocation.TutorAllocationResponse;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.TutorAllocation;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.exception.ResourceNotFoundException;
import com.a9.etutoring.exception.UnauthorizedException;
import com.a9.etutoring.config.AllocationPreviewScheduleProperties;
import com.a9.etutoring.repository.TutorAllocationRepository;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.EmailService;
import com.a9.etutoring.service.TutorAllocationService;
import com.a9.etutoring.util.SecurityContextUtil;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TutorAllocationServiceImpl implements TutorAllocationService {

    private static final Logger logger = LoggerFactory.getLogger(TutorAllocationServiceImpl.class);

    private final UserRepository userRepository;
    private final TutorAllocationRepository tutorAllocationRepository;
    private final EmailService emailService;
    private final AllocationPreviewScheduleProperties scheduleProperties;

    public TutorAllocationServiceImpl(UserRepository userRepository,
                                      TutorAllocationRepository tutorAllocationRepository,
                                      EmailService emailService,
                                      AllocationPreviewScheduleProperties scheduleProperties) {
        this.userRepository = userRepository;
        this.tutorAllocationRepository = tutorAllocationRepository;
        this.emailService = emailService;
        this.scheduleProperties = scheduleProperties;
    }

    @Override
    public TutorAllocationResponse allocate(AllocationCreateRequest request) {
        UserPrincipal admin = SecurityContextUtil.currentPrincipal()
            .orElseThrow(() -> new UnauthorizedException("UNAUTHORIZED", "Authentication required"));
        User student = findActiveUser(request.studentUserId());
        User tutor = findActiveUser(request.tutorUserId());
        validateRoles(student, tutor);
        validateSchedule(request);
        checkScheduleOverlap(request.tutorUserId(), request.scheduleStart(), request.scheduleEnd());
        User allocatedBy = findActiveUser(admin.getId());
        TutorAllocation allocation = createAllocation(student, tutor, allocatedBy, request);
        TutorAllocation saved = tutorAllocationRepository.save(allocation);
        sendAllocationEmails(student, tutor);
        return toResponse(saved);
    }

    @Override
    public List<TutorAllocationResponse> allocateBulk(BulkAllocationRequest request) {
        List<TutorAllocationResponse> result = new ArrayList<>();
        for (AllocationCreateRequest item : request.items()) {
            result.add(allocate(item));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public BulkAllocationPreviewResponse previewBulkAllocation(AllocationPreviewRequest request) {
        SecurityContextUtil.currentPrincipal()
            .orElseThrow(() -> new UnauthorizedException("UNAUTHORIZED", "Authentication required"));
        User tutor = findActiveUser(request.tutorUserId());
        if (tutor.getRole() != UserRole.TUTOR) {
            throw new BadRequestException("INVALID_TUTOR", "User is not a tutor");
        }
        List<User> students = new ArrayList<>();
        for (UUID studentId : request.studentUserIds()) {
            User student = findActiveUser(studentId);
            if (student.getRole() != UserRole.STUDENT) {
                throw new BadRequestException("INVALID_STUDENT", "User is not a student");
            }
            students.add(student);
        }
        ZoneId zone = resolveZone(request.timeZoneId());
        List<Slot> slots = computeSlotsForDay(request.date(), request.slotDurationMinutes(), zone, request.startTime());
        if (request.startTime() == null) {
            slots = slots.stream().filter(s -> s.start().isAfter(Instant.now())).toList();
        }
        if (request.studentUserIds().size() > slots.size()) {
            throw new BadRequestException("TOO_MANY_STUDENTS",
                "Number of students (" + request.studentUserIds().size() + ") exceeds available remaining slots for the day (" + slots.size() + ").");
        }
        List<AllocationPreviewItemResponse> items = new ArrayList<>();
        for (int i = 0; i < request.studentUserIds().size(); i++) {
            Slot slot = slots.get(i);
            String startStr = slot.start().atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String endStr = slot.end().atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            items.add(new AllocationPreviewItemResponse(
                students.get(i).getId(),
                tutor.getId(),
                request.reason(),
                startStr,
                endStr
            ));
        }
        return new BulkAllocationPreviewResponse(items);
    }

    private ZoneId resolveZone(String timeZoneId) {
        if (timeZoneId != null && !timeZoneId.isBlank()) {
            try {
                return ZoneId.of(timeZoneId.trim());
            } catch (DateTimeException e) {
                throw new BadRequestException("INVALID_TIMEZONE", "Invalid timeZoneId: " + timeZoneId);
            }
        }
        return ZoneId.systemDefault();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TutorAllocationResponse> list(Pageable pageable, String search) {
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<TutorAllocation> page = tutorAllocationRepository.findAllBySearch(normalizedSearch, pageable);
        List<TutorAllocationResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    @Override
    public TutorAllocationResponse undo(UUID id) {
        TutorAllocation allocation = tutorAllocationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ALLOCATION_NOT_FOUND", "Allocation not found: " + id));
        if (allocation.getEndedDate() != null) {
            throw new BadRequestException("ALLOCATION_ALREADY_ENDED", "Allocation is already ended");
        }
        allocation.setEndedDate(Instant.now());
        return toResponse(tutorAllocationRepository.save(allocation));
    }

    @Override
    public TutorAllocationResponse update(UUID id, AllocationUpdateRequest request) {
        TutorAllocation allocation = tutorAllocationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ALLOCATION_NOT_FOUND", "Allocation not found: " + id));
        if (allocation.getEndedDate() != null) {
            throw new BadRequestException("CANNOT_UPDATE_ENDED_ALLOCATION", "Cannot update an ended allocation");
        }
        boolean hasReason = request.reason() != null;
        boolean hasSchedule = request.scheduleStart() != null && request.scheduleEnd() != null;
        boolean hasSchedulePartial = (request.scheduleStart() != null) != (request.scheduleEnd() != null);
        boolean hasStudent = request.studentUserId() != null;
        boolean hasTutor = request.tutorUserId() != null;
        if (!hasReason && !hasSchedule && !hasStudent && !hasTutor) {
            throw new BadRequestException("NO_UPDATE_FIELDS", "At least one field must be provided");
        }
        if (hasSchedulePartial) {
            throw new BadRequestException("INVALID_SCHEDULE", "Both scheduleStart and scheduleEnd must be provided together");
        }
        if (hasSchedule && request.scheduleEnd().isBefore(request.scheduleStart())) {
            throw new BadRequestException("INVALID_SCHEDULE", "scheduleEnd must be after or equal to scheduleStart");
        }
        if (hasReason) {
            allocation.setReason(request.reason());
        }
        if (hasSchedule) {
            User tutor = hasTutor ? findActiveUser(request.tutorUserId()) : allocation.getTutor();
            if (hasTutor && tutor.getRole() != UserRole.TUTOR) {
                throw new BadRequestException("INVALID_TUTOR", "User is not a tutor");
            }
            checkScheduleOverlapExcluding(tutor.getId(), request.scheduleStart(), request.scheduleEnd(), allocation.getId());
            allocation.setScheduleStart(request.scheduleStart());
            allocation.setScheduleEnd(request.scheduleEnd());
            if (hasTutor) {
                allocation.setTutor(tutor);
            }
        }
        if (hasStudent) {
            User student = findActiveUser(request.studentUserId());
            if (student.getRole() != UserRole.STUDENT) {
                throw new BadRequestException("INVALID_STUDENT", "User is not a student");
            }
            allocation.setStudent(student);
        }
        if (hasTutor && !hasSchedule) {
            User tutor = findActiveUser(request.tutorUserId());
            if (tutor.getRole() != UserRole.TUTOR) {
                throw new BadRequestException("INVALID_TUTOR", "User is not a tutor");
            }
            allocation.setTutor(tutor);
            checkScheduleOverlapExcluding(tutor.getId(), allocation.getScheduleStart(), allocation.getScheduleEnd(), allocation.getId());
        }
        return toResponse(tutorAllocationRepository.save(allocation));
    }

    private User findActiveUser(UUID id) {
        return userRepository.findByIdAndDeletedDateIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found: " + id));
    }

    private void validateRoles(User student, User tutor) {
        if (student.getRole() != UserRole.STUDENT) {
            throw new BadRequestException("INVALID_STUDENT", "User is not a student");
        }
        if (tutor.getRole() != UserRole.TUTOR) {
            throw new BadRequestException("INVALID_TUTOR", "User is not a tutor");
        }
    }

    private void validateSchedule(AllocationCreateRequest request) {
        if (request.scheduleEnd().isBefore(request.scheduleStart())) {
            throw new BadRequestException("INVALID_SCHEDULE", "scheduleEnd must be after or equal to scheduleStart");
        }
    }

    private void checkScheduleOverlap(UUID tutorId, Instant scheduleStart, Instant scheduleEnd) {
        List<TutorAllocation> overlapping = tutorAllocationRepository.findActiveByTutorWithScheduleOverlap(tutorId, scheduleStart, scheduleEnd);
        if (!overlapping.isEmpty()) {
            throw new BadRequestException("SCHEDULE_OVERLAP", "Tutor already allocated for this schedule");
        }
    }

    private void checkScheduleOverlapExcluding(UUID tutorId, Instant scheduleStart, Instant scheduleEnd, UUID exclusionAllocationId) {
        List<TutorAllocation> overlapping = tutorAllocationRepository.findActiveByTutorWithScheduleOverlapExcluding(
            tutorId, scheduleStart, scheduleEnd, exclusionAllocationId);
        if (!overlapping.isEmpty()) {
            throw new BadRequestException("SCHEDULE_OVERLAP", "Tutor already allocated for this schedule");
        }
    }

    private TutorAllocation createAllocation(User student, User tutor, User allocatedBy, AllocationCreateRequest request) {
        TutorAllocation a = new TutorAllocation();
        a.setId(UUID.randomUUID());
        a.setStudent(student);
        a.setTutor(tutor);
        a.setAllocatedBy(allocatedBy);
        a.setAllocatedDate(Instant.now());
        a.setEndedDate(null);
        a.setReason(request.reason());
        a.setScheduleStart(request.scheduleStart());
        a.setScheduleEnd(request.scheduleEnd());
        return a;
    }

    private TutorAllocationResponse toResponse(TutorAllocation a) {
        return new TutorAllocationResponse(
            a.getId(),
            a.getStudent().getId(),
            a.getTutor().getId(),
            a.getAllocatedBy().getId(),
            a.getAllocatedDate(),
            a.getEndedDate(),
            a.getReason(),
            a.getScheduleStart(),
            a.getScheduleEnd()
        );
    }

    private void sendAllocationEmails(User student, User tutor) {
        String studentFullName = buildFullName(student);
        String tutorFullName = buildFullName(tutor);
        logger.info("Sending allocation emails: studentId={}, tutorId={}", student.getId(), tutor.getId());
        emailService.sendEmail(student.getEmail(),
            "Personal Tutor Allocation – eTutoring",
            buildStudentEmailBody(studentFullName, tutorFullName));
        emailService.sendEmail(tutor.getEmail(),
            "New Student Assigned – eTutoring",
            buildTutorEmailBody(tutorFullName, studentFullName));
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

    private String buildStudentEmailBody(String studentFullName, String tutorFullName) {
        return String.format(
            "Hello %s,\n\n" +
            "You have been assigned a personal tutor:\n" +
            "Tutor: %s\n\n" +
            "Please log in to eTutoring to send messages, arrange meetings, and upload documents.\n\n" +
            "Best regards,\n" +
            "eTutoring System",
            studentFullName, tutorFullName);
    }

    private String buildTutorEmailBody(String tutorFullName, String studentFullName) {
        return String.format(
            "Hello %s,\n\n" +
            "You have been assigned a new student:\n" +
            "Student: %s\n\n" +
            "Please log in to eTutoring to view their dashboard and start communication.\n\n" +
            "Best regards,\n" +
            "eTutoring System",
            tutorFullName, studentFullName);
    }

    private record Slot(Instant start, Instant end) {}

    private List<Slot> computeSlotsForDay(LocalDate date, int slotDurationMinutes, ZoneId zone, LocalTime customStart) {
        int workStart = scheduleProperties.getWorkStartHour();
        int workEnd = scheduleProperties.getWorkEndHour();
        int lunchStart = scheduleProperties.getLunchStartHour();
        int lunchEnd = scheduleProperties.getLunchEndHour();
        LocalTime lunchStartTime = LocalTime.of(lunchStart, 0);
        LocalTime lunchEndTime = LocalTime.of(lunchEnd, 0);
        List<Slot> result = new ArrayList<>();
        LocalDateTime morningEnd = date.atTime(lunchStartTime);
        if (customStart != null) {
            if (customStart.isBefore(lunchStartTime)) {
                addSlots(result, date.atTime(customStart), morningEnd, slotDurationMinutes, zone);
            }
            if (!customStart.isAfter(lunchEndTime)) {
                addSlots(result, date.atTime(lunchEndTime), date.atTime(workEnd, 0), slotDurationMinutes, zone);
            } else {
                addSlots(result, date.atTime(customStart), date.atTime(workEnd, 0), slotDurationMinutes, zone);
            }
        } else {
            addSlots(result, date.atTime(workStart, 0), morningEnd, slotDurationMinutes, zone);
            addSlots(result, date.atTime(lunchEndTime), date.atTime(workEnd, 0), slotDurationMinutes, zone);
        }
        return result;
    }

    private void addSlots(List<Slot> out, LocalDateTime segStart, LocalDateTime segEnd, int periodMinutes, ZoneId zone) {
        LocalDateTime current = segStart;
        while (!current.plusMinutes(periodMinutes).isAfter(segEnd)) {
            LocalDateTime slotEnd = current.plusMinutes(periodMinutes);
            out.add(new Slot(current.atZone(zone).toInstant(), slotEnd.atZone(zone).toInstant()));
            current = slotEnd;
        }
    }
}
