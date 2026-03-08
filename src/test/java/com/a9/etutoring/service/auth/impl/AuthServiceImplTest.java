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
import com.a9.etutoring.service.EmailService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
            userRepository, passwordEncoder, authenticationManager, jwtService,
            emailService, "http://localhost:3000");
    }

    @Test
    void signupShouldPersistUserAndReturnToken() {
        AuthSignupRequest request = new AuthSignupRequest(
            "alice",
            "Alice",
            "Tan",
            "alice@example.com",
            "Password123",
            null
        );

        when(userRepository.existsByUsernameAndDeletedDateIsNull("alice")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedDateIsNull("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.signup(request);

        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(UserRole.STUDENT, response.role());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertNotNull(saved.getId());
        assertEquals("encoded", saved.getPassword());
        assertEquals(UserRole.STUDENT, saved.getRole());
    }

    @Test
    void signupShouldFailOnDuplicateUsername() {
        when(userRepository.existsByUsernameAndDeletedDateIsNull("alice")).thenReturn(true);

        assertThrows(
            DuplicateResourceException.class,
            () -> authService.signup(new AuthSignupRequest("alice", "A", "B", "a@x.com", "Password123", UserRole.STUDENT))
        );
    }

    @Test
    void loginShouldFailWhenUserDoesNotExist() {
        when(userRepository.findByUsernameAndDeletedDateIsNull("ghost")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(
            UnauthorizedException.class,
            () -> authService.login(new AuthLoginRequest("ghost", "Password123"))
        );
        assertEquals("User ghost does not exist. Please sign up.", ex.getMessage());
    }

    @Test
    void loginShouldAuthenticateAndUpdateLastLoginDate() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setFirstName("Alice");
        user.setLastName("Tan");
        user.setPassword("encoded");
        user.setRole(UserRole.STUDENT);
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setCreatedDate(Instant.now());

        UserPrincipal principal = UserPrincipal.fromUser(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(userRepository.findByUsernameAndDeletedDateIsNull("alice")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(eq(principal))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new AuthLoginRequest("alice", "Password123"));

        assertEquals("jwt-token", response.accessToken());
        verify(userRepository).save(any(User.class));
        assertNotNull(user.getLastLoginDate());
    }
}
