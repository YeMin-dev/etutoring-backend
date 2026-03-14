package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.user.UserResponse;
import com.a9.etutoring.exception.UnauthorizedException;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.service.TutorAllocationService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tutor")
public class TutorAllocationController {

    private final TutorAllocationService tutorAllocationService;

    public TutorAllocationController(TutorAllocationService tutorAllocationService) {
        this.tutorAllocationService = tutorAllocationService;
    }

    @GetMapping("/allocated-students")
    public List<UserResponse> listAllocatedStudents(@AuthenticationPrincipal UserPrincipal principal) {
        UUID tutorId = requirePrincipal(principal);
        return tutorAllocationService.listAllocatedStudentsForTutor(tutorId);
    }

    private static UUID requirePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "Authentication required");
        }
        return principal.getId();
    }
}
