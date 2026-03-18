package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.allocation.AllocatedTutorResponse;
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
@RequestMapping("/api/student")
public class StudentAllocationController {

    private final TutorAllocationService tutorAllocationService;

    public StudentAllocationController(TutorAllocationService tutorAllocationService) {
        this.tutorAllocationService = tutorAllocationService;
    }

    @GetMapping("/allocated-tutors")
    public List<AllocatedTutorResponse> listAllocatedTutors(@AuthenticationPrincipal UserPrincipal principal) {
        UUID studentId = requirePrincipal(principal);
        return tutorAllocationService.listAllocatedTutorsForStudent(studentId);
    }

    private static UUID requirePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "Authentication required");
        }
        return principal.getId();
    }
}
