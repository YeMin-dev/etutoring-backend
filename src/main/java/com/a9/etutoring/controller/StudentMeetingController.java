package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.meeting.MeetingResponse;
import com.a9.etutoring.exception.UnauthorizedException;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.MeetingService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
public class StudentMeetingController {

    private final MeetingService meetingService;

    public StudentMeetingController(MeetingService meetingService) {
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
        return meetingService.listForStudent(currentUserId, pageable);
    }

    @GetMapping("/meetings/{id}")
    public MeetingResponse getById(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        return meetingService.getByIdForStudent(requirePrincipal(principal), id);
    }

    private static UUID requirePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "Authentication required");
        }
        return principal.getId();
    }
}
