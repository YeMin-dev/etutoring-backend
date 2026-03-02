package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.dto.user.UserResponse;
import com.a9.etutoring.domain.dto.user.UserUpdateRequest;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.DuplicateResourceException;
import com.a9.etutoring.exception.ResourceNotFoundException;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return toResponse(findActiveById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAllByDeletedDateIsNull()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public UserResponse update(UUID id, UserUpdateRequest req) {
        User user = findActiveById(id);

        if (req.username() != null && !req.username().equals(user.getUsername())) {
            ensureUsernameUnique(req.username(), user.getId());
            user.setUsername(req.username());
        }
        if (req.email() != null && !req.email().equals(user.getEmail())) {
            ensureEmailUnique(req.email(), user.getId());
            user.setEmail(req.email());
        }
        if (req.firstName() != null) {
            user.setFirstName(req.firstName());
        }
        if (req.lastName() != null) {
            user.setLastName(req.lastName());
        }
        if (req.password() != null) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }
        if (req.role() != null) {
            user.setRole(req.role());
        }
        if (req.isActive() != null) {
            user.setIsActive(req.isActive());
        }
        if (req.isLocked() != null) {
            user.setIsLocked(req.isLocked());
        }

        user.setUpdatedDate(Instant.now());
        return toResponse(userRepository.save(user));
    }

    @Override
    public void delete(UUID id) {
        User user = findActiveById(id);
        Instant now = Instant.now();
        user.setDeletedDate(now);
        user.setUpdatedDate(now);
        userRepository.save(user);
    }

    private User findActiveById(UUID id) {
        return userRepository.findByIdAndDeletedDateIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private void ensureUsernameUnique(String username, UUID currentUserId) {
        userRepository.findByUsernameAndDeletedDateIsNull(username)
            .filter(existing -> !existing.getId().equals(currentUserId))
            .ifPresent(existing -> {
                throw new DuplicateResourceException("DUPLICATE_USERNAME", "Username already exists");
            });
    }

    private void ensureEmailUnique(String email, UUID currentUserId) {
        userRepository.findByEmailAndDeletedDateIsNull(email)
            .filter(existing -> !existing.getId().equals(currentUserId))
            .ifPresent(existing -> {
                throw new DuplicateResourceException("DUPLICATE_EMAIL", "Email already exists");
            });
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getRole(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getIsActive(),
            user.getIsLocked(),
            user.getCreatedDate(),
            user.getUpdatedDate(),
            user.getLastLoginDate()
        );
    }
}
