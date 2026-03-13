package com.a9.etutoring.service.auth.impl;

import com.a9.etutoring.domain.dto.auth.AuthLoginRequest;
import com.a9.etutoring.domain.dto.auth.AuthResponse;
import com.a9.etutoring.domain.dto.auth.AuthSignupRequest;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.DuplicateResourceException;
import com.a9.etutoring.exception.UnauthorizedException;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.security.jwt.JwtService;
import com.a9.etutoring.service.auth.AuthService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponse signup(AuthSignupRequest request) {
        if (userRepository.existsByUsernameAndDeletedDateIsNull(request.username())) {
            throw new DuplicateResourceException("DUPLICATE_USERNAME", "Username already exists");
        }
        if (userRepository.existsByEmailAndDeletedDateIsNull(request.email())) {
            throw new DuplicateResourceException("DUPLICATE_EMAIL", "Email already exists");
        }

        Instant now = Instant.now();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(request.username());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role() != null ? request.role() : UserRole.STUDENT);
        user.setIsActive(Boolean.TRUE);
        user.setIsLocked(Boolean.FALSE);
        user.setCreatedDate(now);
        user.setUpdatedDate(null);
        user.setLastLoginDate(null);
        user.setLastInteractionDate(now);
        user.setDeletedDate(null);

        User saved = userRepository.save(user);
        UserPrincipal principal = UserPrincipal.fromUser(saved);
        String token = jwtService.generateToken(principal);
        return toAuthResponse(principal, token);
    }

    @Override
    public AuthResponse login(AuthLoginRequest request) {
        User user = userRepository.findByUsernameAndDeletedDateIsNull(request.username())
            .orElseThrow(() -> new UnauthorizedException(
                "AUTHENTICATION_FAILED",
                "User " + request.username() + " does not exist. Please sign up."
            ));

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            user.setLastLoginDate(Instant.now());
            user.setLastInteractionDate(Instant.now());
            userRepository.save(user);

            String token = jwtService.generateToken(principal);
            return toAuthResponse(principal, token);
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("AUTHENTICATION_FAILED", "Invalid username or password");
        }
    }

    private AuthResponse toAuthResponse(UserPrincipal principal, String token) {
        return new AuthResponse(
            token,
            "Bearer",
            jwtService.getExpirationSeconds(),
            principal.getId(),
            principal.getUsername(),
            principal.getRole()
        );
    }
}
