package com.a9.etutoring.controller;

import com.a9.etutoring.domain.dto.auth.MeResponse;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        Optional<User> userOptional = userRepository.findByIdAndDeletedDateIsNull(principal.getId());
        return userOptional
            .map(user -> new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                Boolean.TRUE.equals(user.getIsActive()),
                Boolean.TRUE.equals(user.getIsLocked()),
                user.getLastLoginDate()
            ))
            .orElseGet(() -> new MeResponse(
                principal.getId(),
                principal.getUsername(),
                principal.getFirstName(),
                principal.getLastName(),
                principal.getEmail(),
                principal.getRole(),
                principal.getIsActive(),
                principal.getIsLocked(),
                null
            ));
    }
}
