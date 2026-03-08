package com.a9.etutoring.service;

import com.a9.etutoring.domain.dto.user.UserCreateRequest;
import com.a9.etutoring.domain.dto.user.UserResponse;
import com.a9.etutoring.domain.dto.user.UserUpdateRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse getById(UUID id);

    UserResponse getAdminUser();

    Page<UserResponse> listStudents(Pageable pageable);

    Page<UserResponse> listTutors(Pageable pageable);

    UserResponse create(UserCreateRequest request);

    UserResponse update(UUID id, UserUpdateRequest req);

    void delete(UUID id);
}
