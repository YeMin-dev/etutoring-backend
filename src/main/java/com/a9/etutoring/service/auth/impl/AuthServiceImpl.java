package com.a9.etutoring.service.auth.impl;

import com.a9.etutoring.domain.dto.auth.AuthLoginRequest;
import com.a9.etutoring.domain.dto.auth.AuthResponse;
import com.a9.etutoring.domain.dto.auth.AuthSignupRequest;
import com.a9.etutoring.domain.dto.auth.ForgotPasswordRequest;
import com.a9.etutoring.domain.dto.auth.ResetPasswordRequest;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.exception.DuplicateResourceException;
import com.a9.etutoring.exception.UnauthorizedException;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.security.UserPrincipal;
import com.a9.etutoring.security.jwt.JwtService;
import com.a9.etutoring.service.EmailService;
import com.a9.etutoring.service.auth.AuthService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
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

    private static final int PASSWORD_RESET_EXPIRY_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final String passwordResetBaseUrl;

    public AuthServiceImpl(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        EmailService emailService,
        @Value("${app.password-reset.base-url:http://localhost:3000}") String passwordResetBaseUrl
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.passwordResetBaseUrl = passwordResetBaseUrl;
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
            userRepository.save(user);

            String token = jwtService.generateToken(principal);
            return toAuthResponse(principal, token);
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("AUTHENTICATION_FAILED", "Invalid username or password");
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailAndDeletedDateIsNull(request.email()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetExpiresAt(Instant.now().plus(PASSWORD_RESET_EXPIRY_MINUTES, ChronoUnit.MINUTES));
            userRepository.save(user);
            String link = passwordResetBaseUrl + "/reset-password?token=" + token;
            String subject = "Reset your eTutoring password";
            String body = String.format(
                "A password reset was requested for your eTutoring account.\n\n" +
                "Click the link below to set a new password:\n%s\n\n" +
                "This link expires in %d minutes. If you did not request this, please ignore this email.\n\n" +
                "Best regards,\neTutoring System",
                link, PASSWORD_RESET_EXPIRY_MINUTES);
            emailService.sendEmail(user.getEmail(), subject, body);
        });
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository
            .findByPasswordResetTokenAndPasswordResetExpiresAtAfterAndDeletedDateIsNull(request.token(), Instant.now())
            .orElseThrow(() -> new BadRequestException("INVALID_OR_EXPIRED_TOKEN", "Invalid or expired reset token"));
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
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
