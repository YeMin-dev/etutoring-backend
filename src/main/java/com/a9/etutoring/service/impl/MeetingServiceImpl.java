package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.dto.meeting.MeetingCreateRequest;
import com.a9.etutoring.domain.dto.meeting.MeetingResponse;
import com.a9.etutoring.domain.dto.meeting.MeetingUpdateRequest;
import com.a9.etutoring.domain.enums.MeetingMode;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.enums.VirtualMeetingPlatform;
import com.a9.etutoring.domain.model.Meeting;
import com.a9.etutoring.domain.model.TutorAllocation;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.exception.ResourceNotFoundException;
import com.a9.etutoring.config.AppProperties;
import com.a9.etutoring.repository.MeetingRepository;
import com.a9.etutoring.repository.TutorAllocationRepository;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.service.EmailService;
import com.a9.etutoring.service.MeetingService;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MeetingServiceImpl implements MeetingService {

    private final DateTimeFormatter emailInstantFormatter;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final TutorAllocationRepository tutorAllocationRepository;
    private final EmailService emailService;

    public MeetingServiceImpl(MeetingRepository meetingRepository,
                              UserRepository userRepository,
                              TutorAllocationRepository tutorAllocationRepository,
                              EmailService emailService,
                              AppProperties appProperties) {
        this.meetingRepository = meetingRepository;
        this.userRepository = userRepository;
        this.tutorAllocationRepository = tutorAllocationRepository;
        this.emailService = emailService;
        this.emailInstantFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(appProperties.getDefaultTimeZone());
    }

    @Override
    public MeetingResponse create(UUID currentUserId, MeetingCreateRequest request) {
        User tutor = userRepository.findByIdAndDeletedDateIsNull(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Tutor not found"));
        if (tutor.getRole() != UserRole.TUTOR) {
            throw new BadRequestException("ONLY_TUTORS_CAN_ARRANGE", "Only tutors can arrange meetings");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("INVALID_SCHEDULE", "endDate must be after or equal to startDate");
        }
        User student = userRepository.findByIdAndDeletedDateIsNull(request.studentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Student not found"));
        if (student.getRole() != UserRole.STUDENT) {
            throw new BadRequestException("INVALID_STUDENT", "User is not a student");
        }
        checkMeetingWithinAllocation(currentUserId, student.getId(), request.startDate(), request.endDate());
        checkMeetingOverlap(currentUserId, request.startDate(), request.endDate(), null);

        String locTrimmed = request.location() != null ? request.location().trim() : "";
        String linkTrimmed = request.link() != null ? request.link().trim() : "";
        validateMeetingModeDetails(request.mode(), locTrimmed, linkTrimmed, request.virtualPlatform());

        Meeting meeting = new Meeting();
        meeting.setId(UUID.randomUUID());
        meeting.setStudent(student);
        meeting.setTutor(tutor);
        meeting.setCreatedBy(tutor);
        meeting.setStartDate(request.startDate());
        meeting.setEndDate(request.endDate());
        meeting.setDescription(request.description());
        switch (request.mode()) {
            case IN_PERSON -> {
                meeting.setMode(MeetingMode.IN_PERSON);
                meeting.setLocation(locTrimmed);
                meeting.setLink(null);
                meeting.setVirtualPlatform(null);
            }
            case VIRTUAL -> {
                meeting.setMode(MeetingMode.VIRTUAL);
                meeting.setLocation(null);
                meeting.setLink(linkTrimmed);
                meeting.setVirtualPlatform(request.virtualPlatform());
            }
        }
        Instant now = Instant.now();
        meeting.setCreatedDate(now);
        meeting.setUpdatedDate(null);

        Meeting saved = meetingRepository.save(meeting);
        sendMeetingArrangedEmail(student, saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MeetingResponse> list(UUID currentUserId, Pageable pageable) {
        Page<Meeting> page = meetingRepository.findByTutor_IdOrderByStartDateDesc(currentUserId, pageable);
        List<MeetingResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MeetingResponse> listForStudent(UUID currentUserId, Pageable pageable) {
        Page<Meeting> page = meetingRepository.findByStudent_IdOrderByStartDateDesc(currentUserId, pageable);
        List<MeetingResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingResponse getById(UUID currentUserId, UUID id) {
        Meeting meeting = meetingRepository.findByIdAndTutor_Id(id, currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("MEETING_NOT_FOUND", "Meeting not found"));
        return toResponse(meeting);
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingResponse getByIdForStudent(UUID currentUserId, UUID id) {
        Meeting meeting = meetingRepository.findByIdAndStudent_Id(id, currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("MEETING_NOT_FOUND", "Meeting not found"));
        return toResponse(meeting);
    }

    @Override
    public MeetingResponse update(UUID currentUserId, UUID id, MeetingUpdateRequest request) {
        Meeting meeting = meetingRepository.findByIdAndTutor_Id(id, currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("MEETING_NOT_FOUND", "Meeting not found"));

        Instant start = request.startDate() != null ? request.startDate() : meeting.getStartDate();
        Instant end = request.endDate() != null ? request.endDate() : meeting.getEndDate();
        MeetingMode mode = request.mode() != null ? request.mode() : meeting.getMode();
        String mergedLoc = request.location() != null ? request.location() : meeting.getLocation();
        String mergedLink = request.link() != null ? request.link() : meeting.getLink();
        VirtualMeetingPlatform mergedVp = request.virtualPlatform() != null ? request.virtualPlatform() : meeting.getVirtualPlatform();

        String locTrimmed = mergedLoc != null ? mergedLoc.trim() : "";
        String linkTrimmed = mergedLink != null ? mergedLink.trim() : "";

        if (end.isBefore(start)) {
            throw new BadRequestException("INVALID_SCHEDULE", "endDate must be after or equal to startDate");
        }
        validateMeetingModeDetails(mode, locTrimmed, linkTrimmed, mergedVp);

        meeting.setStartDate(start);
        meeting.setEndDate(end);
        if (request.description() != null) {
            meeting.setDescription(request.description());
        }
        switch (mode) {
            case IN_PERSON -> {
                meeting.setMode(MeetingMode.IN_PERSON);
                meeting.setLocation(locTrimmed);
                meeting.setLink(null);
                meeting.setVirtualPlatform(null);
            }
            case VIRTUAL -> {
                meeting.setMode(MeetingMode.VIRTUAL);
                meeting.setLocation(null);
                meeting.setLink(linkTrimmed);
                meeting.setVirtualPlatform(mergedVp);
            }
        }
        checkMeetingWithinAllocation(meeting.getTutor().getId(), meeting.getStudent().getId(), meeting.getStartDate(), meeting.getEndDate());
        checkMeetingOverlap(currentUserId, meeting.getStartDate(), meeting.getEndDate(), meeting.getId());
        meeting.setUpdatedDate(Instant.now());
        Meeting saved = meetingRepository.save(meeting);
        sendMeetingUpdatedEmail(saved.getStudent(), saved);
        return toResponse(saved);
    }

    @Override
    public void delete(UUID currentUserId, UUID id) {
        Meeting meeting = meetingRepository.findByIdAndTutor_Id(id, currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("MEETING_NOT_FOUND", "Meeting not found"));
        sendMeetingCancelledEmail(meeting.getStudent(), meeting);
        meetingRepository.delete(meeting);
    }

    private static boolean isHttpInvitationLink(String trimmed) {
        if (trimmed.isEmpty()) {
            return false;
        }
        String lower = trimmed.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private void validateMeetingModeDetails(MeetingMode mode, String locationTrimmed, String linkTrimmed, VirtualMeetingPlatform platform) {
        switch (mode) {
            case IN_PERSON -> {
                if (locationTrimmed.isEmpty()) {
                    throw new BadRequestException("INVALID_MEETING_LOCATION", "In-person meetings require a non-blank location");
                }
                if (!linkTrimmed.isEmpty()) {
                    throw new BadRequestException("INVALID_MEETING_IN_PERSON", "In-person meetings must not include a meeting link");
                }
                if (platform != null) {
                    throw new BadRequestException("INVALID_MEETING_IN_PERSON", "In-person meetings must not include a virtual platform");
                }
            }
            case VIRTUAL -> {
                if (!locationTrimmed.isEmpty()) {
                    throw new BadRequestException("INVALID_MEETING_VIRTUAL", "Virtual meetings must not include a physical location");
                }
                if (linkTrimmed.isEmpty() || !isHttpInvitationLink(linkTrimmed)) {
                    throw new BadRequestException("INVALID_MEETING_LINK", "Virtual meetings require an http(s) invitation link");
                }
                if (platform == null) {
                    throw new BadRequestException("INVALID_MEETING_PLATFORM", "Virtual meetings require a platform");
                }
            }
        }
    }

    private String formatMeetingPlace(Meeting meeting) {
        if (meeting.getMode() == MeetingMode.VIRTUAL) {
            StringBuilder sb = new StringBuilder();
            if (meeting.getVirtualPlatform() != null) {
                sb.append("Platform: ").append(meeting.getVirtualPlatform().displayName()).append('\n');
            }
            sb.append("Link: ").append(meeting.getLink());
            return sb.toString();
        }
        return "Location: " + meeting.getLocation();
    }

    private void checkMeetingWithinAllocation(UUID tutorId, UUID studentId, Instant meetingStart, Instant meetingEnd) {
        List<TutorAllocation> containing = tutorAllocationRepository.findByTutorAndStudentWithScheduleContaining(
            tutorId, studentId, meetingStart, meetingEnd);
        if (containing.isEmpty()) {
            throw new BadRequestException("MEETING_NOT_WITHIN_ALLOCATION", "Meeting must fall within an allocation slot for this student");
        }
    }

    private void checkMeetingOverlap(UUID tutorId, Instant startDate, Instant endDate, UUID exclusionMeetingId) {
        List<Meeting> overlapping = exclusionMeetingId == null
            ? meetingRepository.findByTutorWithScheduleOverlap(tutorId, startDate, endDate)
            : meetingRepository.findByTutorWithScheduleOverlapExcluding(tutorId, startDate, endDate, exclusionMeetingId);
        if (!overlapping.isEmpty()) {
            throw new BadRequestException("MEETING_OVERLAP", "Tutor already has a meeting in this time range");
        }
    }

    private MeetingResponse toResponse(Meeting m) {
        return new MeetingResponse(
            m.getId(),
            m.getStudent().getId(),
            m.getTutor().getId(),
            m.getCreatedBy().getId(),
            m.getStartDate(),
            m.getEndDate(),
            m.getMode(),
            m.getLocation(),
            m.getLink(),
            m.getVirtualPlatform(),
            m.getDescription(),
            m.getCreatedDate(),
            m.getUpdatedDate()
        );
    }

    private void sendMeetingArrangedEmail(User student, Meeting meeting) {
        String studentName = buildFullName(student);
        String startStr = emailInstantFormatter.format(meeting.getStartDate());
        String endStr = emailInstantFormatter.format(meeting.getEndDate());
        String place = formatMeetingPlace(meeting);
        String desc = meeting.getDescription() != null && !meeting.getDescription().isBlank()
            ? "\nDetails: " + meeting.getDescription() : "";

        String body = String.format(
            "Hello %s,\n\n" +
            "A meeting has been arranged for you.\n\n" +
            "Start: %s\nEnd: %s\nMode: %s\n%s%s\n\n" +
            "Best regards,\neTutoring System",
            studentName, startStr, endStr, meeting.getMode(), place, desc);

        emailService.sendEmail(student.getEmail(), "Meeting arranged – eTutoring", body);
    }

    private void sendMeetingUpdatedEmail(User student, Meeting meeting) {
        String studentName = buildFullName(student);
        String startStr = emailInstantFormatter.format(meeting.getStartDate());
        String endStr = emailInstantFormatter.format(meeting.getEndDate());
        String place = formatMeetingPlace(meeting);
        String desc = meeting.getDescription() != null && !meeting.getDescription().isBlank()
            ? "\nDetails: " + meeting.getDescription() : "";

        String body = String.format(
            "Hello %s,\n\n" +
            "Your meeting has been updated.\n\n" +
            "Start: %s\nEnd: %s\nMode: %s\n%s%s\n\n" +
            "Best regards,\neTutoring System",
            studentName, startStr, endStr, meeting.getMode(), place, desc);

        emailService.sendEmail(student.getEmail(), "Meeting updated – eTutoring", body);
    }

    private void sendMeetingCancelledEmail(User student, Meeting meeting) {
        String studentName = buildFullName(student);
        String startStr = emailInstantFormatter.format(meeting.getStartDate());
        String endStr = emailInstantFormatter.format(meeting.getEndDate());

        String body = String.format(
            "Hello %s,\n\n" +
            "Your meeting scheduled for %s to %s has been cancelled.\n\n" +
            "Best regards,\neTutoring System",
            studentName, startStr, endStr);

        emailService.sendEmail(student.getEmail(), "Meeting cancelled – eTutoring", body);
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
}
