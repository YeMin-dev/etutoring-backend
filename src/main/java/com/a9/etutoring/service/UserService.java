package com.a9.etutoring.service;

import com.a9.etutoring.domain.dto.user.UserResponse;
import com.a9.etutoring.domain.dto.user.UserUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse getById(UUID id);

    List<UserResponse> list();

    UserResponse update(UUID id, UserUpdateRequest req);

    void delete(UUID id);
}
