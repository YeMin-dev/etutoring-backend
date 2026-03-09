package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.meeting.MeetingCreateRequest;
import com.a9.etutoring.domain.dto.meeting.MeetingResponse;
import com.a9.etutoring.domain.dto.meeting.MeetingUpdateRequest;
import com.a9.etutoring.exception.UnauthorizedException;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.MeetingService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tutor")
public class TutorMeetingController {

    private final MeetingService meetingService;

    public TutorMeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping("/meetings")
    public Page<MeetingResponse> list(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        UUID currentUserId = requirePrincipal(principal);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "startDate"));
        return meetingService.list(currentUserId, pageable);
    }

    @GetMapping("/meetings/{id}")
    public MeetingResponse getById(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return meetingService.getById(requirePrincipal(principal), id);
    }

    @PostMapping("/meetings")
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingResponse create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody MeetingCreateRequest request
    ) {
        return meetingService.create(requirePrincipal(principal), request);
    }

    @PutMapping("/meetings/{id}")
    public MeetingResponse update(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody MeetingUpdateRequest request
    ) {
        return meetingService.update(requirePrincipal(principal), id, request);
    }

    @DeleteMapping("/meetings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        meetingService.delete(requirePrincipal(principal), id);
    }

    private static UUID requirePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "Authentication required");
        }
        return principal.getId();
    }
}
