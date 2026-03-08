package com.a9.etutoring.service.impl;

import com.a9.etutoring.domain.dto.user.UserCreateRequest;
import com.a9.etutoring.domain.dto.user.UserResponse;
import com.a9.etutoring.domain.dto.user.UserUpdateRequest;
import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.User;
import com.a9.etutoring.exception.BadRequestException;
import com.a9.etutoring.exception.DuplicateResourceException;
import com.a9.etutoring.exception.ResourceNotFoundException;
import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    public UserResponse getAdminUser() {
        return userRepository.findFirstByDeletedDateIsNullAndRoleOrderByCreatedDateAsc(UserRole.ADMIN)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("ADMIN_NOT_FOUND", "Admin user not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listStudents(Pageable pageable) {
        Page<User> page = userRepository.findAllByDeletedDateIsNullAndRole(UserRole.STUDENT, pageable);
        List<UserResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listTutors(Pageable pageable) {
        Page<User> page = userRepository.findAllByDeletedDateIsNullAndRole(UserRole.TUTOR, pageable);
        List<UserResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    @Override
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByUsernameAndDeletedDateIsNull(request.username())) {
            throw new DuplicateResourceException("DUPLICATE_USERNAME", "Username already exists");
        }
        if (userRepository.existsByEmailAndDeletedDateIsNull(request.email())) {
            throw new DuplicateResourceException("DUPLICATE_EMAIL", "Email already exists");
        }
        if (request.role() == UserRole.ADMIN && userRepository.existsByDeletedDateIsNullAndRole(UserRole.ADMIN)) {
            throw new BadRequestException("ONLY_ONE_ADMIN_ALLOWED", "Only one admin user is allowed");
        }
        Instant now = Instant.now();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(request.username());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setIsActive(request.isActive() != null ? request.isActive() : Boolean.TRUE);
        user.setIsLocked(request.isLocked() != null ? request.isLocked() : Boolean.FALSE);
        user.setCreatedDate(now);
        user.setUpdatedDate(null);
        user.setLastLoginDate(null);
        user.setDeletedDate(null);
        User saved = userRepository.save(user);
        return toResponse(saved);
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
            if (req.role() == UserRole.ADMIN && user.getRole() != UserRole.ADMIN
                && userRepository.existsByDeletedDateIsNullAndRoleAndIdNot(UserRole.ADMIN, user.getId())) {
                throw new BadRequestException("ONLY_ONE_ADMIN_ALLOWED", "Only one admin user is allowed");
            }
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
