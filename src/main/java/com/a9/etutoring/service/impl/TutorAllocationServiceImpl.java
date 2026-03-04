package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.dto.allocation.AllocationCreateRequest;
import com.a9.etutoring.domain.dto.allocation.AllocationUpdateRequest;
import com.a9.etutoring.domain.dto.allocation.BulkAllocationRequest;
import com.a9.etutoring.domain.dto.allocation.TutorAllocationResponse;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.TutorAllocation;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.exception.ResourceNotFoundException;
import com.a9.etutoring.exception.UnauthorizedException;
import com.a9.etutoring.repository.TutorAllocationRepository;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.TutorAllocationService;
import com.a9.etutoring.util.SecurityContextUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TutorAllocationServiceImpl implements TutorAllocationService {

    private final UserRepository userRepository;
    private final TutorAllocationRepository tutorAllocationRepository;

    public TutorAllocationServiceImpl(UserRepository userRepository,
                                      TutorAllocationRepository tutorAllocationRepository) {
        this.userRepository = userRepository;
        this.tutorAllocationRepository = tutorAllocationRepository;
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
        return toResponse(tutorAllocationRepository.save(allocation));
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
}
